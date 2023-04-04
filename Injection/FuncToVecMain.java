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

import java.io.IOException;

public class FuncToVecMain {
    static String path = "F:\\bugGenerate\\sourceCode";
    static Class listener = FuncToVec.class;
    public static boolean hasError = false;
    public static StringBuilder output;
    public static String filePath = "F:\\result\\overflow_bug\\2_0x00000000e86b5156e8fd624255bf7a6d722a8f1f.sol_GetOverflowFunc_.txt.txt";

    public static void main(String[] args) throws IllegalAccessException, IOException, InstantiationException {
        walkAST(filePath,listener);
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
