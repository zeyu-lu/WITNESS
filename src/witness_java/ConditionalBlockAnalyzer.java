package witness_java;

import myjava.JavaLexer;
import myjava.JavaParser;
import myjava.JavaParserBaseListener;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.xpath.XPath;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ConditionalBlockAnalyzer extends JavaParserBaseListener {

    private final int targetLineNumber;
    private final CommonTokenStream tokens;
    private final Set<String> assignedVariables = new HashSet<>();
    private String returnOrThrow = "";
    private JavaParser parser; // # parser should be declared as member variable

    public ConditionalBlockAnalyzer(int targetLineNumber, CommonTokenStream tokens) {
        this.targetLineNumber = targetLineNumber;
        this.tokens = tokens;
    }

    public void setParser(JavaParser parser) {
        this.parser = parser;
    }

    public Set<String> getAssignedVariables() {
        return assignedVariables;
    }

    public String getReturnOrThrow() {
        return returnOrThrow;
    }

    @Override
    public void enterStatement(JavaParser.StatementContext ctx) {
        if (ctx.getStart().getLine() <= targetLineNumber && targetLineNumber <= ctx.getStop().getLine() && targetLineNumber == ctx.getStart().getLine()) {
            if (ctx.IF() == null || ctx.WHILE() == null || ctx.SWITCH() == null || ctx.FOR() != null) {
                processStatementBlock(ctx);
            }
        } else if (ctx.getStart().getLine() <= targetLineNumber && targetLineNumber <= ctx.getStop().getLine() && targetLineNumber == ctx.getStop().getLine()) {
            if (ctx.WHILE() == null) {
                processStatementBlock(ctx);
            }
        } else if (ctx.getStart().getLine() <= targetLineNumber && ctx.getStop().getLine() >= targetLineNumber) {
            if (ctx.IF() != null) {
                Token start = ctx.IF().getSymbol();
                Token stop = ctx.parExpression().stop;

                if (start != null && stop != null) {
                    int startLine = start.getLine();
                    int stopLine = stop.getLine();

                    // Assuming `givenLineNumber` is the line number you want to check
                    if (targetLineNumber >= startLine && targetLineNumber <= stopLine) {
                        processStatementBlock(ctx);
                    }
                }
            }
            if (ctx.WHILE() != null) {
                Token start = ctx.WHILE().getSymbol();
                Token stop = ctx.parExpression().stop;

                if (start != null && stop != null) {
                    int startLine = start.getLine();
                    int stopLine = stop.getLine();

                    // Assuming `targetLineNumber` is the line number you want to check
                    if (targetLineNumber >= startLine && targetLineNumber <= stopLine) {
                        processStatementBlock(ctx);
                    }
                }
            }

        }
    }

    private void processStatementBlock(JavaParser.StatementContext ctx) {
        boolean containsReturn = false;
        boolean containsThrow = false;

        for (JavaParser.StatementContext statementContext : ctx.statement()) {
            for (int i = 0; i < statementContext.getChildCount(); i++) {
                ParseTree outerChild = statementContext.getChild(i);
                for (int j = 0; j < outerChild.getChildCount(); j++) {
                    ParseTree child = outerChild.getChild(j);

                    if (child instanceof ParserRuleContext) {
                        Interval interval = child.getSourceInterval();
                        String text = tokens.getText(interval);

                        if (text.contains("return")) {
                            containsReturn = true;
                        }
                        if (text.contains("throw")) {
                            containsThrow = true;
                        }
                    }
                }
            }
        }

        if (containsReturn && containsThrow) {
            returnOrThrow = "RETURN-THROW";
        } else if (containsReturn) {
            returnOrThrow = "RETURN";
        } else if (containsThrow) {
            returnOrThrow = "THROW";
        } else {
            for (JavaParser.StatementContext statementContext : ctx.statement()) {
                for (int i = 0; i < statementContext.getChildCount(); i++) {
                    ParseTree outerChild = statementContext.getChild(i);
                    for (int j = 0; j < outerChild.getChildCount(); j++) {
                        ParseTree child = outerChild.getChild(j);

                        if (child instanceof ParserRuleContext) {
                            Interval interval = child.getSourceInterval();
                            String text = tokens.getText(interval);

                            Collection<ParseTree> expressions = unwrapSingleChildChains(child);
                            for (ParseTree expr : expressions) {
                                handleExpressionOrStatement(expr);
                            }

                            handleLocalVariableDeclaration(child);
                        }
                    }
                }
            }
        }
    }

    // # return a list of ParseTree, using XPath
    private Collection<ParseTree> unwrapSingleChildChains(ParseTree node) {
        return XPath.findAll(node, "//blockStatement/statement/*", parser);
    }

    private void handleExpressionOrStatement(ParseTree child) {
        if (child instanceof JavaParser.ExpressionContext) {
            analyzeExpression((JavaParser.ExpressionContext) child);
        } else if (child instanceof JavaParser.StatementContext) {
            JavaParser.ExpressionContext exprCtx = ((JavaParser.StatementContext) child).expression(0);
            if (exprCtx != null) {
                analyzeExpression(exprCtx);
            }
        }

    }

    private void analyzeExpression(JavaParser.ExpressionContext exprCtx) {
        if (exprCtx.ASSIGN() != null || exprCtx.INC() != null || exprCtx.DEC() != null
                || exprCtx.AND_ASSIGN() != null || exprCtx.OR_ASSIGN() != null || exprCtx.XOR_ASSIGN() != null
                || exprCtx.ADD_ASSIGN() != null || exprCtx.SUB_ASSIGN() != null
                || exprCtx.MUL_ASSIGN() != null || exprCtx.DIV_ASSIGN() != null
                || exprCtx.MOD_ASSIGN() != null
                || exprCtx.LSHIFT_ASSIGN() != null || exprCtx.RSHIFT_ASSIGN() != null || exprCtx.URSHIFT_ASSIGN() != null) {
            String assignedVariable = exprCtx.expression(0).getText();
            assignedVariables.add(assignedVariable);
        }
    }

    private void handleLocalVariableDeclaration(ParseTree node) {
        Collection<ParseTree> localVarDecls = XPath.findAll(node, "//blockStatement/localVariableDeclaration/*", parser);
        for (ParseTree decl : localVarDecls) {
            if (decl instanceof JavaParser.VariableDeclaratorsContext) {
                JavaParser.VariableDeclaratorsContext variableDeclaratorsContext = (JavaParser.VariableDeclaratorsContext) decl;
                for (JavaParser.VariableDeclaratorContext varDeclCtx : variableDeclaratorsContext.variableDeclarator()) {
                    String variableName = varDeclCtx.variableDeclaratorId().getText();
                    assignedVariables.add(variableName);
                }
            }
        }
    }

}