package pre;

import gen.SolidityLexer;
import gen.SolidityParser;
import listener.InvaildCheck;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.TreeMap;
import utils.*;

public class ExtractUnhandledException {
    static String path = "F:\\result\\UnhandledException_func";
    //    static String path = "F:\\bugGenerate\\sourceCode";
    static Class listener = InvaildCheck.class;
    public static boolean hasError = false;
    private static int contract_count;
    private static int snippet_count;
    public static TreeMap<Integer,String> targetExpression = new TreeMap<Integer,String>();

    public static void main(String[] args) throws IllegalAccessException, IOException, InstantiationException {

        long startTime = System.currentTimeMillis();
        File file = new File(path);
        File[] fileArray = file.listFiles();
        for (int i = 0; i < fileArray.length; ++i) {
            String filePath = fileArray[i].getPath();
            if (fileArray[i].isFile()) {
                String fileName = fileArray[i].getName();
                hasError = false;
                String code = CharStreams.fromFileName(filePath).toString();
                targetExpression.clear();
                walkAST(filePath, listener);

                if (!hasError) {
                    if (!targetExpression.isEmpty()) {
//                        ++contract_count;
                        try {
                            utils.WriteFile("unhandledException_deleted", filePath);
                            NavigableMap nm = targetExpression.descendingMap();
                            for(Object s:nm.values()){
                                String[] arr = s.toString().split("\\?");
    //                            System.out.println(Arrays.toString(arr));
                                String part1 = code.substring(0, Integer.parseInt(arr[1]));
                                String part2 = code.substring(Integer.parseInt(arr[2])+1);
                                String newExpression = arr[0];
                                code = part1 + newExpression + part2;
                            }
//                        System.out.println(code);
                            utils.WriteFile(fileName,code);
                        } catch (Exception e) {
                            System.out.println(filePath);
                        }

                    }
                }
            }

        }

        long endTime = System.currentTimeMillis( );
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
