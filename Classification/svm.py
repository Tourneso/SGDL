import joblib
import sklearn
from sklearn import svm
import numpy as np
from sklearn.model_selection import train_test_split

path='allContract_vec.txt'
data=np.loadtxt(path,dtype=float,delimiter=',',encoding='utf-8-sig')

#x是数据，y为标签
x,y=np.split(data,indices_or_sections=(149,),axis=1)

train_data,test_data,train_label,test_label=sklearn.model_selection.train_test_split(x,y,random_state=1,train_size=0.6,test_size=0.4)

classifier=svm.SVC(C=1,kernel='rbf',gamma=1,decision_function_shape='ovr')#设置训练器
classifier.fit(train_data,train_label.ravel())#对训练集部分进行训练

print("train：",classifier.score(train_data,train_label))
print("text：",classifier.score(test_data,test_label))

joblib.dump(classifier,'classifier.model')


