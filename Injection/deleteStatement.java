package pre;

import gen.SolidityLexer;
import gen.SolidityParser;
import listener.UncheckedSend;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import utils.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.TreeMap;


public class deleteStatement {

    static String path = "F:\\result\\UnhandledException_func";
    //    static String path = "F:\\bugGenerate\\sourceCode";
    static Class listener = UncheckedSend.class;
    public static boolean hasError = false;
    private static int contract_count;
    private static int snippet_count;
    public static TreeMap<Integer,String> targetStatement = new TreeMap<Integer,String>();

    public static void main(String[] args) throws IllegalAccessException, IOException, InstantiationException {

        long startTime = System.currentTimeMillis();
        File file = new File(path);
        File[] fileArray = file.listFiles();
        for (int i = 0; i < fileArray.length; ++i) {
            String filePath = fileArray[i].getPath();
            if (fileArray[i].isFile()) {
                hasError = false;
                String fileName = fileArray[i].getName();
                String code = CharStreams.fromFileName(filePath).toString();
                targetStatement.clear();
                walkAST(filePath, listener);
                if (!hasError) {
                    if (!targetStatement.isEmpty()) {
//                        System.out.println(targetStatement);
//                        ++contract_count;
                        utils.WriteFile("unchecked_deleted", filePath);
                        NavigableMap nm = targetStatement.descendingMap();
                        for(Object s:nm.values()){
                            String[] arr = s.toString().split(",");
                            code = code.substring(0, Integer.parseInt(arr[0]))+
                                    code.substring(Integer.parseInt(arr[1])+1);
                        }
//                        System.out.println(code);
                        utils.WriteFile((snippet_count++)+fileName+"uncheckedsend", code);
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
