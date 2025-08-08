package witness_java;

import myjava.JavaParser;
import myjava.JavaParserBaseListener;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

public class CustomListener extends JavaParserBaseListener {
    private int targetLineNumber;
    private int currentNestingLevel = 0;
    private Integer nestingLevelAtLine = null;

    public CustomListener(int lineNumber) {
        this.targetLineNumber = lineNumber;
    }

    @Override
    public void enterStatement(JavaParser.StatementContext ctx) {
        if (ctx.IF() != null){
            changeNestingLevel(ctx, true);
        }

        if (ctx.FOR() != null){
            changeNestingLevel(ctx, true);
        }

        if (ctx.WHILE() != null){
            changeNestingLevel(ctx, true);
        }

        if (ctx.SWITCH() != null){
            changeNestingLevel(ctx, true);
        }

        if (ctx.SYNCHRONIZED() != null){
            changeNestingLevel(ctx, true);
        }
    }

    @Override
    public void exitStatement(JavaParser.StatementContext ctx) {
        if (ctx.IF() != null){
            changeNestingLevel(ctx, false);
        }

        if (ctx.FOR() != null){
            changeNestingLevel(ctx, false);
        }

        if (ctx.WHILE() != null){
            changeNestingLevel(ctx, false);
        }

        if (ctx.SWITCH() != null){
            changeNestingLevel(ctx, false);
        }

        if (ctx.SYNCHRONIZED() != null){
            changeNestingLevel(ctx, false);
        }

    }

    private void changeNestingLevel(ParserRuleContext ctx, boolean entering) {
        if (ctx.getStart().getLine() <= targetLineNumber && ctx.getStop().getLine() >= targetLineNumber) {
            currentNestingLevel += entering ? 1 : -1;
        }
    }

    @Override
    public void visitTerminal(TerminalNode node) {
        if (node.getSymbol().getLine() == targetLineNumber) {
            nestingLevelAtLine = currentNestingLevel;
        }
    }

    public int getResult() {
        return nestingLevelAtLine != null ? nestingLevelAtLine : 0;
    }
}