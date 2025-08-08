package witness_java;

import myjava.JavaLexer;
import myjava.JavaParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public class ConditionalNestingLevelDetector {

    public int getNestingLevel(String javaClass, int lineNumber) throws Exception {
        CharStream charStream = CharStreams.fromString(javaClass);
        JavaLexer lexer = new JavaLexer(charStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JavaParser parser = new JavaParser(tokens);

        ParseTree tree = parser.compilationUnit();

        CustomListener listener = new CustomListener(lineNumber);
        ParseTreeWalker.DEFAULT.walk(listener, tree);

        return listener.getResult();
    }

}
