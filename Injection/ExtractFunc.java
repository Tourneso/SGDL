package pre;

import gen.SolidityLexer;
import gen.SolidityParser;
import listener.*;
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

public class ExtractFunc {
    static String path = "F:\\result\\test";
    //    static String path = "F:\\bugGenerate\\sourceCode";
    static Class listener = Timestamp.class; //ShortAddress、GetOverflowFunc、UnhandledException、Timestamp
    public static boolean hasError = false;
    private static int contract_count;
    private static int snippet_count;
    public static HashSet<String> targetFunction = new HashSet<String>();

    public static void main(String[] args) throws IllegalAccessException, IOException, InstantiationException {

        String type = listener.getName().split("\\.")[1];
        System.out.println(type);
        long startTime = System.currentTimeMillis();
        File file = new File(path);
        File[] fileArray = file.listFiles();
        for (int i = 0; i < fileArray.length; ++i) {
            String filePath = fileArray[i].getPath();
            if (fileArray[i].isFile()) {
                String fileName = fileArray[i].getName();
                hasError = false;
                String code = CharStreams.fromFileName(filePath).toString();
                targetFunction.clear();
                walkAST(filePath, listener);

                if (!hasError) {
                    if (!targetFunction.isEmpty()) {
                        ++contract_count;
                        utils.WriteFile(type+"_contracts", filePath);
                        for (String s : targetFunction) {
                            String[] arr = s.split(",");
                            String snippet = code.substring((Integer.parseInt(arr[0])), Integer.parseInt(arr[1]) + 1);
//                            System.out.println(snippet);
                            utils.WriteFile((snippet_count++)+"_"+fileName+"_"+type+"_", snippet);
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
        ParseTree tree = parser.sourceUnit();
//        ParseTree tree = parser.functionDefinition();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk((ParseTreeListener) listener.newInstance(), tree);
    }
}

