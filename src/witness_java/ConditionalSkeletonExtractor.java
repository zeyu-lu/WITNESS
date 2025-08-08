package witness_java;

import myjava.JavaLexer;
import myjava.JavaParser;
import myjava.JavaParserBaseListener;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConditionalSkeletonExtractor extends JavaParserBaseListener {
    private final CharStream input;
    private final int lineNumber;
    private boolean conditionFound = false;
    private String storedCondition;
    private TokenStreamRewriter rewriter;
    private String originalCondition;
    private String updatedCondition;
    private int conditionStartLine = -1;

    public ConditionalSkeletonExtractor(CharStream input, int lineNumber) {
        this.input = input;
        this.lineNumber = lineNumber;
    }

    // Constructor for analyzing the rewritten code
    public ConditionalSkeletonExtractor(CharStream input, int lineNumber, TokenStream tokens, String originalCondition, String updatedCondition) {
        this.input = input;
        this.lineNumber = lineNumber;
        this.rewriter = new TokenStreamRewriter(tokens);
        this.originalCondition = originalCondition;
        this.updatedCondition = updatedCondition;
    }

    @Override
    public void enterStatement(JavaParser.StatementContext ctx) {
        // Check if rewriter is initialized, indicating that rewriting is intended
        if (rewriter != null) {
            rewriteCondition(ctx);
        } else {
            if (ctx.getStart().getLine() <= lineNumber && lineNumber <= ctx.getStop().getLine() && lineNumber == ctx.getStart().getLine()) {
                if (ctx.IF() != null || ctx.WHILE() != null || ctx.SWITCH() != null) {
                    String condition = getParCondition(ctx.parExpression());
                    HexToDecimalConverter converter = new HexToDecimalConverter();
                    condition = converter.convertHexToDecimal(condition);
                    this.storedCondition = condition;
                    conditionFound = true;
                    conditionStartLine = ctx.parExpression().getStart().getLine();
                } else if (ctx.RETURN() != null) {
                    if (ctx.expression(0) != null) {
                        String condition = getReturnCondition(ctx.expression(0));
                        HexToDecimalConverter converter = new HexToDecimalConverter();
                        condition = converter.convertHexToDecimal(condition);
                        this.storedCondition = condition;
                        conditionFound = true;
                        conditionStartLine = ctx.expression(0).getStart().getLine();
                    }
                } else if (ctx.FOR() != null) {
                    JavaParser.ForControlContext forCtrl = ctx.forControl();
                    String condition = null;
                    if (forCtrl.enhancedForControl() != null) {
                        condition = getForCondition(forCtrl.enhancedForControl().expression());
                    } else if (forCtrl.expression() != null) {
                        condition = getForCondition(forCtrl.expression());
                    }
                    if (condition != null) {
                        HexToDecimalConverter converter = new HexToDecimalConverter();
                        condition = converter.convertHexToDecimal(condition);
                        this.storedCondition = condition;
                        conditionFound = true;
                        conditionStartLine = ctx.getStart().getLine();  // Set line
                    }
                }
            } else if (ctx.getStart().getLine() <= lineNumber && lineNumber <= ctx.getStop().getLine() && lineNumber == ctx.getStop().getLine()) {
                if (ctx.WHILE() != null) {
                    String condition = getParCondition(ctx.parExpression());
                    HexToDecimalConverter converter = new HexToDecimalConverter();
                    condition = converter.convertHexToDecimal(condition);
                    this.storedCondition = condition;
                    conditionFound = true;
                    conditionStartLine = ctx.parExpression().getStart().getLine();
                }else if (ctx.RETURN() != null) {
                    if (ctx.expression(0) != null) {
                        String condition = getReturnCondition(ctx.expression(0));
                        HexToDecimalConverter converter = new HexToDecimalConverter();
                        condition = converter.convertHexToDecimal(condition);
                        this.storedCondition = condition;
                        conditionFound = true;
                        conditionStartLine = ctx.expression(0).getStart().getLine();
                    }
                }
            } else if (ctx.getStart().getLine() <= lineNumber && ctx.getStop().getLine() >= lineNumber) {
                if (ctx.IF() != null) {
                    Token start = ctx.IF().getSymbol();
                    Token stop = ctx.parExpression().stop;

                    if (start != null && stop != null) {
                        int startLine = start.getLine();
                        int stopLine = stop.getLine();

                        // Assuming `givenLineNumber` is the line number you want to check
                        if (lineNumber >= startLine && lineNumber <= stopLine) {
                            String condition = getParCondition(ctx.parExpression());
                            HexToDecimalConverter converter = new HexToDecimalConverter();
                            condition = converter.convertHexToDecimal(condition);
                            this.storedCondition = condition;
                            conditionFound = true;
                            conditionStartLine = ctx.parExpression().getStart().getLine();
                        }
                    }
                }else if (ctx.WHILE() != null) {
                    Token start = ctx.WHILE().getSymbol();
                    Token stop = ctx.parExpression().stop;

                    if (start != null && stop != null) {
                        int startLine = start.getLine();
                        int stopLine = stop.getLine();

                        // Assuming `targetLineNumber` is the line number you want to check
                        if (lineNumber >= startLine && lineNumber <= stopLine) {
                            String condition = getParCondition(ctx.parExpression());
                            HexToDecimalConverter converter = new HexToDecimalConverter();
                            condition = converter.convertHexToDecimal(condition);
                            this.storedCondition = condition;
                            conditionFound = true;
                            conditionStartLine = ctx.parExpression().getStart().getLine();
                        }
                    }
                }else if (ctx.RETURN() != null) {
                    if (ctx.expression(0) != null) {
                        String condition = getReturnCondition(ctx.expression(0));
                        HexToDecimalConverter converter = new HexToDecimalConverter();
                        condition = converter.convertHexToDecimal(condition);
                        this.storedCondition = condition;
                        conditionFound = true;
                        conditionStartLine = ctx.expression(0).getStart().getLine();
                    }
                }
            }
        }

    }

    public String getStoredCondition() {
        return storedCondition;
    }

    public int getConditionStartLine() {
        return conditionStartLine;
    }

    public void rewriteCondition(JavaParser.StatementContext ctx) {
        if (ctx.getStart().getLine() <= lineNumber && lineNumber <= ctx.getStop().getLine() && lineNumber == ctx.getStart().getLine()) {
            if (ctx.IF() != null || ctx.WHILE() != null || ctx.SWITCH() != null) {
                JavaParser.ParExpressionContext parExprCtx = ctx.parExpression();
                String condition = getParCondition(parExprCtx);
                HexToDecimalConverter converter = new HexToDecimalConverter();
                condition = converter.convertHexToDecimal(condition);
                if (condition.equals(originalCondition)) {
                    // Replace the entire condition within the parentheses
                    rewriter.replace(parExprCtx.start, parExprCtx.stop, updatedCondition);
                }
            } else if (ctx.RETURN() != null) {
                if (ctx.expression(0) != null) {
                    JavaParser.ExpressionContext ExprCtx = ctx.expression(0);
                    String condition = getReturnCondition(ExprCtx);
                    HexToDecimalConverter converter = new HexToDecimalConverter();
                    condition = converter.convertHexToDecimal(condition);
                    if (condition.equals(originalCondition)) {
                        rewriter.replace(ExprCtx.start, ExprCtx.stop, updatedCondition);
                    }
                }
            } else if (ctx.FOR() != null) {
                JavaParser.ForControlContext forCtrl = ctx.forControl();
                if (forCtrl.enhancedForControl() != null) {
                    String forCondition = getForCondition(forCtrl.enhancedForControl().expression());
                    HexToDecimalConverter converter = new HexToDecimalConverter();
                    forCondition = converter.convertHexToDecimal(forCondition);
                    if (forCondition.equals(originalCondition)) {
                        rewriter.replace(forCtrl.expression().start, forCtrl.expression().stop, updatedCondition);
                    }
                } else if (forCtrl.expression() != null) {
                    String forCondition = getForCondition(forCtrl.expression());
                    HexToDecimalConverter converter = new HexToDecimalConverter();
                    forCondition = converter.convertHexToDecimal(forCondition);
                    if (forCondition.equals(originalCondition)) {
                        rewriter.replace(forCtrl.expression().start, forCtrl.expression().stop, updatedCondition);
                    }
                }
            }
            // Additional handling for other types of statements, if necessary
        }else if (ctx.getStart().getLine() <= lineNumber && lineNumber <= ctx.getStop().getLine() && lineNumber == ctx.getStop().getLine()) {
            if (ctx.WHILE() != null) {
                JavaParser.ParExpressionContext parExprCtx = ctx.parExpression();
                String condition = getParCondition(parExprCtx);
                HexToDecimalConverter converter = new HexToDecimalConverter();
                condition = converter.convertHexToDecimal(condition);
                if (condition.equals(originalCondition)) {
                    rewriter.replace(parExprCtx.start, parExprCtx.stop, updatedCondition);
                }
            }else if (ctx.RETURN() != null) {
                if (ctx.expression(0) != null) {
                    JavaParser.ExpressionContext ExprCtx = ctx.expression(0);
                    String condition = getReturnCondition(ExprCtx);
                    HexToDecimalConverter converter = new HexToDecimalConverter();
                    condition = converter.convertHexToDecimal(condition);
                    if (condition.equals(originalCondition)) {
                        rewriter.replace(ExprCtx.start, ExprCtx.stop, updatedCondition);
                    }
                }
            }
        } else if (ctx.getStart().getLine() <= lineNumber && ctx.getStop().getLine() >= lineNumber) {
            if (ctx.IF() != null) {
                Token start = ctx.IF().getSymbol();
                Token stop = ctx.parExpression().stop;

                if (start != null && stop != null) {
                    int startLine = start.getLine();
                    int stopLine = stop.getLine();

                    // Assuming `givenLineNumber` is the line number you want to check
                    if (lineNumber >= startLine && lineNumber <= stopLine) {
                        JavaParser.ParExpressionContext parExprCtx = ctx.parExpression();
                        String condition = getParCondition(parExprCtx);
                        HexToDecimalConverter converter = new HexToDecimalConverter();
                        condition = converter.convertHexToDecimal(condition);
                        if (condition.equals(originalCondition)) {
                            // Replace the entire condition within the parentheses
                            rewriter.replace(parExprCtx.start, parExprCtx.stop, updatedCondition);
                        }
                    }
                }
            }else if (ctx.WHILE() != null) {
                Token start = ctx.WHILE().getSymbol();
                Token stop = ctx.parExpression().stop;

                if (start != null && stop != null) {
                    int startLine = start.getLine();
                    int stopLine = stop.getLine();

                    // Assuming `targetLineNumber` is the line number you want to check
                    if (lineNumber >= startLine && lineNumber <= stopLine) {
                        JavaParser.ParExpressionContext parExprCtx = ctx.parExpression();
                        String condition = getParCondition(parExprCtx);
                        HexToDecimalConverter converter = new HexToDecimalConverter();
                        condition = converter.convertHexToDecimal(condition);
                        if (condition.equals(originalCondition)) {
                            // Replace the entire condition within the parentheses
                            rewriter.replace(parExprCtx.start, parExprCtx.stop, updatedCondition);
                        }
                    }
                }
            }else if (ctx.RETURN() != null) {
                if (ctx.expression(0) != null) {
                    JavaParser.ExpressionContext ExprCtx = ctx.expression(0);
                    String condition = getReturnCondition(ExprCtx);
                    HexToDecimalConverter converter = new HexToDecimalConverter();
                    condition = converter.convertHexToDecimal(condition);
                    if (condition.equals(originalCondition)) {
                        rewriter.replace(ExprCtx.start, ExprCtx.stop, updatedCondition);
                    }
                }
            }
        }
    }

    public String getModifiedCode() {
        return rewriter.getText();
    }

    public String getParCondition(JavaParser.ParExpressionContext ctx) {
        int start = ctx.start.getStartIndex();
        int stop = ctx.stop.getStopIndex();
        Interval interval = new Interval(start, stop);
        return input.getText(interval);
    }

    public String getReturnCondition(JavaParser.ExpressionContext ctx) {
        int start = ctx.start.getStartIndex();
        int stop = ctx.stop.getStopIndex();
        Interval interval = new Interval(start, stop);
        return input.getText(interval);
    }

    public String getForCondition(JavaParser.ExpressionContext expressionCtx) {
        int start = expressionCtx.start.getStartIndex();
        int stop = expressionCtx.stop.getStopIndex();
        Interval interval = new Interval(start, stop);
        return input.getText(interval);
    }

//    public void processCondition(String condition) {
//        String lowerLevelSkeleton = transformToSkeleton(condition);
////        String higherLevelSkeleton = abstractToHigherLevel(lowerLevelSkeleton);
//        System.out.println("Condition: " + condition);
//        System.out.println("Lower Level Skeleton: " + lowerLevelSkeleton);
////        System.out.println("Higher Level Skeleton: " + higherLevelSkeleton);
//    }

    public boolean isConditionFound() {
        return conditionFound;
    }

    public String transformToSkeleton(String condition, boolean isEqual) {
        JavaLexer lexer = new JavaLexer(CharStreams.fromString(condition));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JavaParser parser = new JavaParser(tokens);
        ParseTree tree = parser.expression();

        ExpressionTransformer transformer = new ExpressionTransformer(isEqual);
        return transformer.visit(tree);
    }

    public static String replaceInSpecificLine(String originalCondition, String target, String replacement,
                                               int startLine, int changedLine) {
        String[] lines = originalCondition.split("\n", -1);
        int relativeLineIndex = changedLine - startLine;

        if (relativeLineIndex >= 0 && relativeLineIndex < lines.length) {
            lines[relativeLineIndex] = replaceWithSafeLiteralSupport(lines[relativeLineIndex], target, replacement);
        }

        return String.join("\n", lines);
    }

    public static String replaceWithSafeLiteralSupport(String line, String target, String replacement) {
        int index = line.indexOf(target);
        while (index != -1) {
            boolean validStart = (index == 0) || isBoundaryCharBefore(line.charAt(index - 1));
            int afterIndex = index + target.length();

            boolean validEnd = (afterIndex == line.length()) || isAcceptableAfterLiteral(line, afterIndex, target);

            if (validStart && validEnd) {
                return line.substring(0, index) + replacement + line.substring(afterIndex);
            }

            index = line.indexOf(target, index + 1);
        }

        return line;
    }

    private static boolean isBoundaryCharBefore(char ch) {
        return Character.isWhitespace(ch) || ch == '(' || ch == '=' || ch == '!' || ch == '&' || ch == '|';
    }

    private static boolean isAcceptableAfterLiteral(String line, int afterIndex, String target) {
        if (afterIndex >= line.length()) return true;

        char nextChar = line.charAt(afterIndex);

        // Allow numeric suffixes if the target ends in a digit or decimal point
        if (Character.isDigit(target.charAt(target.length() - 1)) || target.endsWith(".")) {
            return nextChar == 'd' || nextChar == 'f' || nextChar == 'D' || nextChar == 'F'
                    || isBoundaryCharBefore(nextChar); // reuse "before" as boundary after too
        }

        return isBoundaryCharBefore(nextChar);
    }

}
