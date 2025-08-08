package witness_java;

import myjava.JavaLexer;
import myjava.JavaParser;
import myjava.JavaParserBaseListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public class StatementTypeIdentifier extends JavaParserBaseListener {

    private int targetLineNumber;
    private TokenStream tokens;
    private String type = "Unknown";

    public StatementTypeIdentifier(int line, TokenStream tokens) {
        this.targetLineNumber = line;
        this.tokens = tokens;
    }

    public String getType() {
        return type;
    }

    @Override
    public void enterMemberDeclaration(JavaParser.MemberDeclarationContext ctx) {
        if (ctx.getStart().getLine() <= targetLineNumber && ctx.getStop().getLine() >= targetLineNumber) {
            // Get the start and stop tokens for the current context
            int startTokenIndex = ctx.getStart().getTokenIndex();
            int stopTokenIndex = ctx.getStop().getTokenIndex();

            // Get the text of the original source code with spaces
            String originalText = tokens.getText(Interval.of(startTokenIndex, stopTokenIndex));
            System.out.println("enterMemberDeclaration: " + originalText);

            type = "MemberDeclaration";
        }
    }

    @Override
    public void enterLocalVariableDeclaration(JavaParser.LocalVariableDeclarationContext ctx) {
        if (ctx.getStart().getLine() <= targetLineNumber && ctx.getStop().getLine() >= targetLineNumber) {
//            System.out.println("enterLocalVariableDeclaration: " + ctx.getText());

            // Check if the parent context is not a forInit
            if (!(ctx.getParent() instanceof JavaParser.ForInitContext)) {
                // Get the start and stop tokens for the current context
                int startTokenIndex = ctx.getStart().getTokenIndex();
                int stopTokenIndex = ctx.getStop().getTokenIndex();

                // Get the text of the original source code with spaces
                String originalText = tokens.getText(Interval.of(startTokenIndex, stopTokenIndex));
                System.out.println("enterLocalVariableDeclaration: " + originalText);

                type = "LocalVariableDeclaration";
            }
        }
    }

    @Override
    public void enterStatement(JavaParser.StatementContext ctx) {

        if (ctx.getStart().getLine() <= targetLineNumber && ctx.getStop().getLine() >= targetLineNumber) {
            // Get the start and stop tokens for the current context
            int startTokenIndex = ctx.getStart().getTokenIndex();
            int stopTokenIndex = ctx.getStop().getTokenIndex();

            // Get the text of the original source code with spaces
            String originalText = tokens.getText(Interval.of(startTokenIndex, stopTokenIndex));
            System.out.println("enterStatement: " + originalText);

            // Check for different types of statements
            if (ctx.ASSERT() != null) {
                type = "ASSERT";
                return;
            }
            if (ctx.IF() != null) {
                type = "IF";
                return;
            }
            if (ctx.FOR() != null) {
                type = "FOR";
                return;
            }
            if (ctx.WHILE() != null) {
                type = "WHILE";
                return;
            }
            if (ctx.TRY() != null) {
                type = "TRY";
                return;
            }
            if (ctx.SWITCH() != null) {
                type = "SWITCH";
                return;
            }
            if (ctx.YIELD() != null) {
                type = "YIELD";
                return;
            }
            if (ctx.RETURN() != null) {
                type = "RETURN";
                return;
            }
            if (ctx.THROW() != null) {
                type = "THROW";
                return;
            }
            if (ctx.BREAK() != null) {
                type = "BREAK";
                return;
            }
            if (ctx.CONTINUE() != null) {
                type = "CONTINUE";
                return;
            }
            if (ctx.SYNCHRONIZED() != null) {
                type = "SYNCHRONIZED";
                return;
            }
            if (ctx.statementExpression != null) {
                if (ctx.statementExpression.methodCall() != null) {
                    if (ctx.statementExpression.methodCall().SUPER() != null) {
                        type = "methodCall-super";
                    } else if (ctx.statementExpression.methodCall().THIS() != null) {
                        type = "methodCall-this";
                    } else if (ctx.statementExpression.methodCall().getText().trim().startsWith("set")) {
                        type = "methodCall-set";
                    } else if (ctx.statementExpression.methodCall().getText().trim().startsWith("get")) {
                        type = "methodCall-get";
                    } else {
                        type = "methodCall";
                    }
                    return;
                }
                if (ctx.statementExpression.ASSIGN() != null) {
                    type = "ASSIGN";
                    return;
                }
                if (ctx.statementExpression.INC() != null) {
                    type = "INC";
                    return;
                }
                if (ctx.statementExpression.DEC() != null) {
                    type = "DEC";
                    return;
                }
                if (ctx.statementExpression.AND_ASSIGN() != null) {
                    type = "AND_ASSIGN";
                    return;
                }
                if (ctx.statementExpression.OR_ASSIGN() != null) {
                    type = "OR_ASSIGN";
                    return;
                }
                if (ctx.statementExpression.XOR_ASSIGN() != null) {
                    type = "XOR_ASSIGN";
                    return;
                }
                if (ctx.statementExpression.ADD_ASSIGN() != null) {
                    type = "ADD_ASSIGN";
                    return;
                }
                if (ctx.statementExpression.SUB_ASSIGN() != null) {
                    type = "SUB_ASSIGN";
                    return;
                }
                if (ctx.statementExpression.MUL_ASSIGN() != null) {
                    type = "MUL_ASSIGN";
                    return;
                }
                if (ctx.statementExpression.DIV_ASSIGN() != null) {
                    type = "DIV_ASSIGN";
                    return;
                }
                if (ctx.statementExpression.MOD_ASSIGN() != null) {
                    type = "MOD_ASSIGN";
                    return;
                }
                if (ctx.statementExpression.LSHIFT_ASSIGN() != null) {
                    type = "LSHIFT_ASSIGN";
                    return;
                }
                if (ctx.statementExpression.RSHIFT_ASSIGN() != null) {
                    type = "RSHIFT_ASSIGN";
                    return;
                }
                if (ctx.statementExpression.URSHIFT_ASSIGN() != null) {
                    type = "URSHIFT_ASSIGN";
                    return;
                }
            }
            if (ctx.expression() != null) {
                if (ctx.expression(0) != null) {
                    type = "expression";
                    return;
                }
            }
        }

    }

}
