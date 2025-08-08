package witness_java;

import myjava.JavaLexer;
import myjava.JavaParser;
import myjava.JavaParserBaseListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ReturnThrowVariableExtractor {

    private final Map<String, Set<String>> result;

    public ReturnThrowVariableExtractor() {
        result = new HashMap<>();
        result.put("RETURN", new HashSet<>());
        result.put("THROW", new HashSet<>());
    }

    public Map<String, Set<String>> getResult() {
        return result;
    }

    public class StatementListener extends JavaParserBaseListener {
        private final Map<String, Set<String>> resultMap;

        public StatementListener(Map<String, Set<String>> resultMap) {
            this.resultMap = resultMap;
        }

        @Override
        public void enterStatement(JavaParser.StatementContext ctx) {
            if (ctx.RETURN() != null) {
                Set<String> returnVars = new HashSet<>();
                for (JavaParser.ExpressionContext expr : ctx.expression()) {
                    ParseTreeWalker.DEFAULT.walk(new VariableExtractorListener(returnVars), expr);
                }
                resultMap.get("RETURN").addAll(returnVars);
            }
            if (ctx.THROW() != null) {
                Set<String> throwVars = new HashSet<>();
                for (JavaParser.ExpressionContext expr : ctx.expression()) {
                    ParseTreeWalker.DEFAULT.walk(new VariableExtractorListener(throwVars), expr);
                }
                resultMap.get("THROW").addAll(throwVars);
            }
        }
    }

    public class VariableExtractorListener extends JavaParserBaseListener {
        private final Set<String> variables;

        public VariableExtractorListener(Set<String> variables) {
            this.variables = variables;
        }

        @Override
        public void enterPrimary(JavaParser.PrimaryContext ctx) {
            if (ctx.identifier() != null) {
                variables.add(ctx.identifier().getText());
            }
        }
    }

}