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

import listener.*;
import utils.*;

public class Inject {
    static Class listener =  InjectListener.class;
    public static boolean hasError = false;
    public static ArrayList<Integer> indexList = new ArrayList<Integer>();
    public static String filePath = "src/main/resources/test.sol";

    public static void main(String[] args) throws IllegalAccessException, IOException, InstantiationException {
        walkAST(filePath,listener);
        String code = utils.fileToString(filePath);
        int subLoc = indexList.get(0);
        String part1 = code.substring(0,subLoc+1);
        String part2 = code.substring(subLoc+1);
        String sinppet = utils.fileToString("src/main/resources/snippet.txt");
        String injectedCode = part1+"\n"+sinppet+part2;
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
