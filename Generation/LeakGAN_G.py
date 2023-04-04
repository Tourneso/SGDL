import math
import time
import torch
import torch.nn as nn
import torch.nn.functional as F
import config as cfg
from utils.helpers import truncated_normal_

dis_num_filters = [100, 200, 200, 200, 200, 100, 100, 100, 100, 100, 160, 160]
goal_out_size = sum(dis_num_filters)


class LeakGAN_G(nn.Module):
    def __init__(self, embedding_dim, hidden_dim, vocab_size, max_seq_len, padding_idx, goal_size,
                 step_size, gpu=False):
        super(LeakGAN_G, self).__init__()
        self.name = 'leakgan'

        self.hidden_dim = hidden_dim
        self.embedding_dim = embedding_dim
        self.max_seq_len = max_seq_len
        self.vocab_size = vocab_size
        self.padding_idx = padding_idx
        self.goal_size = goal_size
        self.goal_out_size = goal_out_size
        self.step_size = step_size
        self.gpu = gpu
        self.temperature = 1.5

        self.embeddings = nn.Embedding(vocab_size, embedding_dim, padding_idx=padding_idx)
        self.worker = nn.LSTM(embedding_dim, hidden_dim)
        self.manager = nn.LSTM(goal_out_size, hidden_dim)

        self.work2goal = nn.Linear(hidden_dim, vocab_size * goal_size)
        self.mana2goal = nn.Linear(hidden_dim, goal_out_size)
        self.goal2goal = nn.Linear(goal_out_size, goal_size, bias=False)

        self.goal_init = nn.Parameter(torch.rand((cfg.batch_size, goal_out_size)))

        self.init_params()

    def forward(self, idx, inp, work_hidden, mana_hidden, feature, real_goal, no_log=False, train=False):

        emb = self.embeddings(inp).unsqueeze(0)


        mana_out, mana_hidden = self.manager(feature, mana_hidden)
        mana_out = self.mana2goal(mana_out.permute([1, 0, 2]))
        cur_goal = F.normalize(mana_out, dim=-1)
        _real_goal = self.goal2goal(real_goal)
        _real_goal = F.normalize(_real_goal, p=2, dim=-1).unsqueeze(-1)


        work_out, work_hidden = self.worker(emb, work_hidden)
        work_out = self.work2goal(work_out).view(-1, self.vocab_size,
                                                 self.goal_size)


        out = torch.matmul(work_out, _real_goal).squeeze(-1)


        if idx > 1:
            if train:
                temperature = 1.0
            else:
                temperature = self.temperature
        else:
            temperature = self.temperature

        out = temperature * out

        if no_log:
            out = F.softmax(out, dim=-1)
        else:
            out = F.log_softmax(out, dim=-1)

        return out, cur_goal, work_hidden, mana_hidden

    def sample(self, num_samples, batch_size, dis, start_letter=cfg.start_letter, train=False):
        num_batch = num_samples // batch_size + 1 if num_samples != batch_size else 1
        samples = torch.zeros(num_batch * batch_size, self.max_seq_len).long()
        fake_sentences = torch.zeros((batch_size, self.max_seq_len))

        for b in range(num_batch):
            leak_sample, _, _, _ = self.forward_leakgan(fake_sentences, dis, if_sample=True, no_log=False
                                                        , start_letter=start_letter, train=False)

            assert leak_sample.shape == (batch_size, self.max_seq_len)
            samples[b * batch_size:(b + 1) * batch_size, :] = leak_sample

        samples = samples[:num_samples, :]

        return samples

    def pretrain_loss(self, target, dis, start_letter=cfg.start_letter):
        batch_size, seq_len = target.size()
        _, feature_array, goal_array, leak_out_array = self.forward_leakgan(target, dis, if_sample=False, no_log=False,
                                                                            start_letter=start_letter)

        mana_cos_loss = self.manager_cos_loss(batch_size, feature_array, goal_array)
        manager_loss = -torch.sum(mana_cos_loss) / (batch_size * (seq_len // self.step_size))


        work_nll_loss = self.worker_nll_loss(target, leak_out_array)
        work_loss = torch.sum(work_nll_loss) / (batch_size * seq_len)

        return manager_loss, work_loss

    def adversarial_loss(self, target, rewards, dis, start_letter=cfg.start_letter):
        batch_size, seq_len = target.size()
        _, feature_array, goal_array, leak_out_array = self.forward_leakgan(target, dis, if_sample=False, no_log=False,
                                                                            start_letter=start_letter, train=True)
        t0 = time.time()
        mana_cos_loss = self.manager_cos_loss(batch_size, feature_array, goal_array)
        mana_loss = -torch.sum(rewards * mana_cos_loss) / (batch_size * (seq_len // self.step_size))

        work_nll_loss = self.worker_nll_loss(target, leak_out_array)
        work_cos_reward = self.worker_cos_reward(feature_array, goal_array)
        work_loss = -torch.sum(work_nll_loss * work_cos_reward) / (batch_size * seq_len)

        return mana_loss, work_loss

    def manager_cos_loss(self, batch_size, feature_array, goal_array):
        sub_feature = torch.zeros(batch_size, self.max_seq_len // self.step_size, self.goal_out_size)
        real_goal = torch.zeros(batch_size, self.max_seq_len // self.step_size, self.goal_out_size)
        for i in range(self.max_seq_len // self.step_size):
            idx = i * self.step_size
            sub_feature[:, i, :] = feature_array[:, idx + self.step_size, :] - feature_array[:, idx, :]

            if i == 0:
                real_goal[:, i, :] = self.goal_init[:batch_size, :]
            else:
                idx = (i - 1) * self.step_size + 1
                real_goal[:, i, :] = torch.sum(goal_array[:, idx:idx + 4, :], dim=1)

        sub_feature = F.normalize(sub_feature, p=2, dim=-1)
        real_goal = F.normalize(real_goal, p=2, dim=-1)

        cos_loss = F.cosine_similarity(sub_feature, real_goal, dim=-1)

        return cos_loss

    def worker_nll_loss(self, target, leak_out_array):

        loss_fn = nn.NLLLoss(reduction='none')
        loss = loss_fn(leak_out_array.permute([0, 2, 1]), target)

        return loss

    def worker_cos_reward(self, feature_array, goal_array):

        for i in range(int(self.max_seq_len / self.step_size)):
            real_feature = feature_array[:, i * self.step_size, :].unsqueeze(1).expand((-1, self.step_size, -1))
            feature_array[:, i * self.step_size:(i + 1) * self.step_size, :] = real_feature
            if i > 0:
                sum_goal = torch.sum(goal_array[:, (i - 1) * self.step_size:i * self.step_size, :], dim=1, keepdim=True)
            else:
                sum_goal = goal_array[:, 0, :].unsqueeze(1)
            goal_array[:, i * self.step_size:(i + 1) * self.step_size, :] = sum_goal.expand((-1, self.step_size, -1))

        offset_feature = feature_array[:, 1:, :]
        goal_array = goal_array[:, :self.max_seq_len, :]
        sub_feature = offset_feature - goal_array

        sub_feature = F.normalize(sub_feature, p=2, dim=-1)
        all_goal = F.normalize(goal_array, p=2, dim=-1)

        cos_loss = F.cosine_similarity(sub_feature, all_goal, dim=-1)
        return cos_loss

    def forward_leakgan(self, sentences, dis, if_sample, no_log=False, start_letter=cfg.start_letter, train=False):

        batch_size, seq_len = sentences.size()

        feature_array = torch.zeros((batch_size, seq_len + 1, self.goal_out_size))
        goal_array = torch.zeros((batch_size, seq_len + 1, self.goal_out_size))
        leak_out_array = torch.zeros((batch_size, seq_len + 1, self.vocab_size))

        samples = torch.zeros(batch_size, seq_len + 1).long()
        work_hidden = self.init_hidden(batch_size)
        mana_hidden = self.init_hidden(batch_size)
        leak_inp = torch.LongTensor([start_letter] * batch_size)
        real_goal = self.goal_init[:batch_size, :]

        if self.gpu:
            feature_array = feature_array.cuda()
            goal_array = goal_array.cuda()
            leak_out_array = leak_out_array.cuda()

        goal_array[:, 0, :] = real_goal
        for i in range(seq_len + 1):
            if if_sample:
                dis_inp = samples[:, :seq_len]
            else:
                dis_inp = torch.zeros(batch_size, seq_len).long()
                if i > 0:
                    dis_inp[:, :i] = sentences[:, :i]
                    leak_inp = sentences[:, i - 1]

            if self.gpu:
                dis_inp = dis_inp.cuda()
                leak_inp = leak_inp.cuda()
            feature = dis.get_feature(dis_inp).unsqueeze(0)
            feature_array[:, i, :] = feature.squeeze(0)
            out, cur_goal, work_hidden, mana_hidden = self.forward(i, leak_inp, work_hidden, mana_hidden, feature,
                                                                   real_goal, no_log=no_log, train=train)
            leak_out_array[:, i, :] = out
            goal_array[:, i, :] = cur_goal.squeeze(1)
            if i > 0 and i % self.step_size == 0:
                real_goal = torch.sum(goal_array[:, i - 3:i + 1, :], dim=1)
                if i / self.step_size == 1:
                    real_goal += self.goal_init[:batch_size, :]

            if not no_log:
                out = torch.exp(out)
            out = torch.multinomial(out, 1).view(-1)
            samples[:, i] = out.data
            leak_inp = out

        samples = samples[:, :seq_len]
        leak_out_array = leak_out_array[:, :seq_len, :]
        return samples, feature_array, goal_array, leak_out_array

    def batchNLLLoss(self, target, dis, start_letter=cfg.start_letter):
        _, _, _, leak_out_array = self.forward_leakgan(target, dis, if_sample=False, no_log=False,
                                                       start_letter=start_letter)

        nll_loss = torch.mean(self.worker_nll_loss(target, leak_out_array))

        return nll_loss

    def init_hidden(self, batch_size=1):
        h = torch.zeros(1, batch_size, self.hidden_dim)
        c = torch.zeros(1, batch_size, self.hidden_dim)

        if self.gpu:
            return h.cuda(), c.cuda()
        else:
            return h, c

    def init_goal(self, batch_size):
        goal = torch.rand((batch_size, self.goal_out_size)).normal_(std=0.1)
        goal = nn.Parameter(goal)

        if self.gpu:
            return goal.cuda()
        else:
            return goal

    def split_params(self):
        mana_params = list()
        work_params = list()

        mana_params += list(self.manager.parameters())
        mana_params += list(self.mana2goal.parameters())
        mana_params.append(self.goal_init)

        work_params += list(self.embeddings.parameters())
        work_params += list(self.worker.parameters())
        work_params += list(self.work2goal.parameters())
        work_params += list(self.goal2goal.parameters())

        return mana_params, work_params

    def init_params(self):
        for param in self.parameters():
            if param.requires_grad and len(param.shape) > 0:
                stddev = 1 / math.sqrt(param.shape[0])
                if cfg.gen_init == 'uniform':
                    torch.nn.init.uniform_(param, a=-0.05, b=0.05)
                elif cfg.gen_init == 'normal':
                    torch.nn.init.normal_(param, std=stddev)
                elif cfg.gen_init == 'truncated_normal':
                    truncated_normal_(param, std=stddev)
