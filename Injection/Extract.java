package pre;

import gen.SolidityLexer;
import gen.SolidityParser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import listener.*;
import utils.*;

public class Extract {
    static String path = "F:\\bugGenerate\\sourceCode";
    static Class listener = ShortAddress.class;
    public static boolean hasError = false;
    private static int func_count;
    private static int contract_count;

    public static ArrayList<HashMap<String,String>> targetFunction = new ArrayList<HashMap<String,String>>();
    public static HashMap<String, String> targetName = new HashMap<String, String>();
    public static StringBuilder output;

    public static void main(String[] args) throws IllegalAccessException, IOException, InstantiationException {
        long startTime = System.currentTimeMillis( );
        File file = new File(path);
        File[] fileArray = file.listFiles();
        for (int i = 0; i < fileArray.length; ++i){
            String filePath = fileArray[i].getPath();
            if(fileArray[i].isFile()){
                hasError = false;

             String code = CharStreams.fromFileName(filePath).toString();
             targetFunction.clear();
             walkAST(filePath,listener);
              if(!hasError){
                  if(!targetFunction.isEmpty()){
                     ++contract_count;
                     utils.WriteFile("shortAddress_contracts",filePath);
                  }
                  for(HashMap<String,String> target:targetFunction){
                      String[] loc = target.get("WholeFunction").split(",");
                      String snippet = code.substring(Integer.parseInt(loc[0]),Integer.parseInt(loc[1])+1)+"}";
//                      outputSnippet(snippet);
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

    static void walkAST_String(String code,Class listener) throws IOException, IllegalAccessException, InstantiationException {
        CharStream input = CharStreams.fromString(code);
        SolidityLexer lexer = new SolidityLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SolidityParser parser = new SolidityParser(tokens);
        ParseTree tree = parser.functionDefinition();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk((ParseTreeListener) listener.newInstance(), tree);
    }

}
