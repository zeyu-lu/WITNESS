package witness_java;

import myjava.JavaLexer;
import myjava.JavaParser;
import myjava.JavaParserBaseListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class JavaParentContextFinder extends JavaParserBaseListener {
    private final CommonTokenStream tokens;
    private final int targetLineNumber;
    private String parentContextType = "";
    private boolean enteredTargetBlock = false;
    private double relativePosition = 0.0;

    public JavaParentContextFinder(CommonTokenStream tokens, int targetLineNumber) {
        this.tokens = tokens;
        this.targetLineNumber = targetLineNumber;
    }

    public String getParentContextType() {
        return parentContextType;
    }

    public double getRelativePosition() {
        return relativePosition;
    }

    @Override
    public void enterLocalVariableDeclaration(JavaParser.LocalVariableDeclarationContext ctx) {
        if (!enteredTargetBlock &&
                ctx.getStart().getLine() == targetLineNumber &&
                ctx.getStart().getLine() <= targetLineNumber &&
                ctx.getStop().getLine() >= targetLineNumber) {

            if (!(ctx.getParent() instanceof JavaParser.ForInitContext)) {
                for (JavaParser.VariableDeclaratorContext varDeclCtx : ctx.variableDeclarators().variableDeclarator()) {
                    String variableName = varDeclCtx.variableDeclaratorId().getText();
                }

                analyzeContext(ctx);
            }
        }else if (ctx.getStart().getLine() <= targetLineNumber && ctx.getStop().getLine() >= targetLineNumber) {
            // Check if the parent context is not a forInit
            if (!(ctx.getParent() instanceof JavaParser.ForInitContext)) {
                for (JavaParser.VariableDeclaratorContext varDeclCtx : ctx.variableDeclarators().variableDeclarator()) {
                    String variableName = varDeclCtx.variableDeclaratorId().getText();
                }

                analyzeContext(ctx);
            }
        }
    }


    @Override
    public void enterStatement(JavaParser.StatementContext ctx) {
        if (!enteredTargetBlock && ctx.getStart().getLine() <= targetLineNumber && ctx.getStop().getLine() >= targetLineNumber && ctx.getStart().getLine() == targetLineNumber) {
            int startTokenIndex = ctx.getStart().getTokenIndex();
            int stopTokenIndex = ctx.getStop().getTokenIndex();

            if (ctx.RETURN() != null || ctx.THROW() != null) {
                analyzeContext(ctx);
            }

            if (ctx.ASSERT() != null ||
                    ctx.IF() != null ||
                    ctx.FOR() != null ||
                    ctx.WHILE() != null ||
                    ctx.TRY() != null ||
                    ctx.SWITCH() != null ||
                    ctx.YIELD() != null ||
                    ctx.BREAK() != null ||
                    ctx.CONTINUE() != null ||
                    ctx.SYNCHRONIZED() != null) {
                analyzeContext(ctx);
            }

            if (ctx.statementExpression != null) {
                if (ctx.statementExpression.methodCall() != null
                        || ctx.statementExpression.ASSIGN() != null
                        || ctx.statementExpression.INC() != null
                        || ctx.statementExpression.DEC() != null
                        || ctx.statementExpression.AND_ASSIGN() != null
                        || ctx.statementExpression.OR_ASSIGN() != null
                        || ctx.statementExpression.XOR_ASSIGN() != null
                        || ctx.statementExpression.ADD_ASSIGN()  != null
                        || ctx.statementExpression.SUB_ASSIGN()  != null
                        || ctx.statementExpression.MUL_ASSIGN() != null
                        || ctx.statementExpression.DIV_ASSIGN() != null
                        || ctx.statementExpression.MOD_ASSIGN() != null
                        || ctx.statementExpression.LSHIFT_ASSIGN() != null
                        || ctx.statementExpression.RSHIFT_ASSIGN() != null
                        || ctx.statementExpression.URSHIFT_ASSIGN() != null){
                    analyzeContext(ctx);
                }
            }
        }
    }

    @Override
    public void exitStatement(JavaParser.StatementContext ctx) {
        if (!enteredTargetBlock &&
                ctx.getStart().getLine() <= targetLineNumber &&
                ctx.getStop().getLine() >= targetLineNumber) {

            int startTokenIndex = ctx.getStart().getTokenIndex();
            int stopTokenIndex = ctx.getStop().getTokenIndex();

            if (ctx.RETURN() != null || ctx.THROW() != null) {
                analyzeContext(ctx);
            }

            if (ctx.ASSERT() != null ||
                ctx.IF() != null ||
                ctx.FOR() != null ||
                ctx.WHILE() != null ||
                ctx.TRY() != null ||
                ctx.SWITCH() != null ||
                ctx.YIELD() != null ||
                ctx.BREAK() != null ||
                ctx.CONTINUE() != null ||
                ctx.SYNCHRONIZED() != null) {
                analyzeContext(ctx);
            }

            if (ctx.statementExpression != null) {
                if (ctx.statementExpression.methodCall() != null
                    || ctx.statementExpression.ASSIGN() != null
                    || ctx.statementExpression.INC() != null
                    || ctx.statementExpression.DEC() != null
                    || ctx.statementExpression.AND_ASSIGN() != null
                    || ctx.statementExpression.OR_ASSIGN() != null
                    || ctx.statementExpression.XOR_ASSIGN() != null
                    || ctx.statementExpression.ADD_ASSIGN()  != null
                    || ctx.statementExpression.SUB_ASSIGN()  != null
                    || ctx.statementExpression.MUL_ASSIGN() != null
                    || ctx.statementExpression.DIV_ASSIGN() != null
                    || ctx.statementExpression.MOD_ASSIGN() != null
                    || ctx.statementExpression.LSHIFT_ASSIGN() != null
                    || ctx.statementExpression.RSHIFT_ASSIGN() != null
                    || ctx.statementExpression.URSHIFT_ASSIGN() != null){
                    analyzeContext(ctx);
                }
            }

        }
    }

    private void analyzeContext(ParserRuleContext ctx) {
        ParserRuleContext parent = ctx;

        // Step 1: Go up to enclosing block
        while (parent != null && !(parent instanceof JavaParser.SwitchBlockStatementGroupContext || parent instanceof JavaParser.BlockContext)) {
            parent = parent.getParent();
        }

        if (parent != null) {
            parent = parent.getParent();
        }

        // Step 3: Skip over trivial wrappers
        while (parent != null && parent.getChildCount() == 1) {
            parent = parent.getParent();
        }

        // Step 4: Extract keyword from first child or recognize method
        if (parent != null && parent.getChildCount() > 0) {
            if (parent instanceof JavaParser.ConstructorDeclarationContext) {
                parentContextType = "ConstructorDeclaration";
                relativePosition = 1.0 * (targetLineNumber - parent.start.getLine()) / (parent.stop.getLine() - parent.start.getLine());
            } else if (parent instanceof JavaParser.MethodDeclarationContext) {
                parentContextType = "MethodDeclaration";
                relativePosition = 1.0 * (targetLineNumber - parent.start.getLine()) / (parent.stop.getLine() - parent.start.getLine());
            } else {
                String keyword = parent.getChild(0).getText();
                System.out.println("keyword: " + keyword);
                if (keyword.equals("{")) {
                    parentContextType = "Block";
                    relativePosition = 1.0 * (targetLineNumber - parent.start.getLine()) / (parent.stop.getLine() - parent.start.getLine());
                } else if (keyword.contains("case") || keyword.contains("default")) {
                    parent = parent.getParent();
                    keyword = parent.getChild(0).getText();
                    parentContextType = keyword.toUpperCase();
                    relativePosition = 1.0 * (targetLineNumber - parent.start.getLine()) / (parent.stop.getLine() - parent.start.getLine());
                } else {
                    parentContextType = keyword.toUpperCase();
                    relativePosition = 1.0 * (targetLineNumber - parent.start.getLine()) / (parent.stop.getLine() - parent.start.getLine());
                }
            }
        } else {
            parentContextType = "UNKNOWN";
        }

        enteredTargetBlock = true;
    }

}