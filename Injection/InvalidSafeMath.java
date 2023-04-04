package pre;

import gen.SolidityLexer;
import gen.SolidityParser;
import listener.OverFlow;
import listener.TxOrigin;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import utils.*;

public class InvalidSafeMath {
    static String path = "F:\\result\\overflow_func";
//    static String path = "F:\\bugGenerate\\sourceCode";
    static Class listener = OverFlow.class;
    public static boolean hasError = false;
    private static int snippet_count;
    public static TreeMap<Integer,String> targetExpression = new TreeMap<Integer,String>();

    public static void main(String[] args) throws IllegalAccessException, IOException, InstantiationException {

        long startTime = System.currentTimeMillis( );
        File file = new File(path);
        File[] fileArray = file.listFiles();
        for (int i = 0; i < fileArray.length; ++i) {
            String filePath = fileArray[i].getPath();
            String fileName = fileArray[i].getName();
            if (fileArray[i].isFile()) {
                hasError = false;
                String code = CharStreams.fromFileName(filePath).toString();
                targetExpression.clear();
                walkAST(filePath, listener);

                if(!hasError){
                    try {
                        NavigableMap nm = targetExpression.descendingMap();
                        for(Object s:nm.values()){
                            String[] arr = s.toString().split(",");
                            String part1 = code.substring(0, Integer.parseInt(arr[1]));
                            String part2 = code.substring(Integer.parseInt(arr[2])+1);
                            String newExpression = arr[0];
                            code = part1 + newExpression + part2;
                        }
                        utils.WriteFile(fileName,code);
                    } catch (Exception e) {
                    }
                }
            }
        }

    }
    static void walkAST(String filePath,Class listener) throws IOException, IllegalAccessException, InstantiationException {
        CharStream input = CharStreams.fromFileName(filePath);
        SolidityLexer lexer = new SolidityLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SolidityParser parser = new SolidityParser(tokens);
//        ParseTree tree = parser.sourceUnit();
        ParseTree tree = parser.functionDefinition();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk((ParseTreeListener) listener.newInstance(), tree);
    }
}
