package pre;

import gen.SolidityLexer;
import gen.SolidityParser;
import listener.ContractToVec;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import utils.utils;


public class Main {
    static Class listener = ContractToVec.class;
    public static boolean hasError = false;
    public static StringBuilder contractVector;
    private static int count;

    public static void main(String[] args) throws IllegalAccessException, IOException, InstantiationException {
        String[] path = utils.fileToString("F:\\result\\shortAddress_bug\\shortAddress_contracts.txt").split("\n");
        for (int i = 0; i < path.length; i++) {
                String fixedPath = path[i].substring(0,path[i].length()-1);
                String code = utils.fileToString(fixedPath);
                hasError = false;
                contractVector = new StringBuilder();
                walkAST_String(code,listener);
                if(!hasError){
                    count++;
                    utils.WriteFile("shortAddress_seq",contractVector.toString());
                }
        }


    }
    static void walkAST_String(String code,Class listener) throws IOException, IllegalAccessException, InstantiationException {
        CharStream input = CharStreams.fromString(code);
        SolidityLexer lexer = new SolidityLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SolidityParser parser = new SolidityParser(tokens);
        ParseTree tree = parser.sourceUnit();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk((ParseTreeListener) listener.newInstance(), tree);
    }
}
