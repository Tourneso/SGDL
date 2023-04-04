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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class ExtractTx {
    static String path = "F:\\result\\test";
    static Class listener = TxOrigin.class;
    public static boolean hasError = false;
    private static int contract_count;
    private static int snippet_count;
    public static HashSet<String> targetFunction = new HashSet<String>();
    public static HashSet<String> targetIfStatement = new HashSet<String>();

    public static void main(String[] args) throws IllegalAccessException, IOException, InstantiationException {

        long startTime = System.currentTimeMillis( );
        File file = new File(path);
        File[] fileArray = file.listFiles();
        for (int i = 0; i < fileArray.length; ++i){
            String filePath = fileArray[i].getPath();
            if(fileArray[i].isFile()){
                hasError = false;
                String fileName = fileArray[i].getName();
                String code = CharStreams.fromFileName(filePath).toString();
                targetFunction.clear();
                targetIfStatement.clear();
                walkAST(filePath,listener);

                if(!hasError){
                    if(!targetFunction.isEmpty()){

                        ++contract_count;
                        utils.WriteFile("txorigin_contracts",filePath);

                      for(String s:targetFunction){
                          String[] arr = s.split(",");
                          String snippet = code.substring((Integer.parseInt(arr[1])),Integer.parseInt(arr[2])+1);
//                          if(arr[0].equals("msg")){
//                              snippet.replaceFirst("msg.sender","tx.origin");
//                          }
//                          System.out.println(snippet);
                          utils.WriteFile((snippet_count++)+"_tx_"+fileName,snippet);
                        }
//                        for(String s:targetIfStatement){
//                            String[] arr = s.split(",");
//                            String snippet = code.substring((Integer.parseInt(arr[1])),Integer.parseInt(arr[2])+1);
//                            if(arr[0].equals("msg")){
//                                snippet = snippet.replaceFirst("msg.sender","tx.origin");
//
//                            }
////                            System.out.println(snippet);
//                            utils.WriteFile("tx_snippet"+snippet_count++,snippet);
//                        }
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
