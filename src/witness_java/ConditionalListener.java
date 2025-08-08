package witness_java;

import myjava.JavaParser;
import myjava.JavaParserBaseListener;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.misc.Interval;

public class ConditionalListener extends JavaParserBaseListener {
    private final int targetLineNumber;
    private final TokenStream tokens;
    private boolean insideTargetBlock = false;
    private boolean matched = false;
    private int nestingLevel = 0;
    private int conditionalCount = 0;
    private String extractedBlock = null;

    private int blockStartIndex = -1;  // token index range of matched block
    private int blockStopIndex = -1;

    public ConditionalListener(int targetLineNumber, TokenStream tokens) {
        this.targetLineNumber = targetLineNumber;
        this.tokens = tokens;
    }

    @Override
    public void enterStatement(JavaParser.StatementContext ctx) {
        int startLine = ctx.getStart().getLine();
        int stopLine = ctx.getStop().getLine();

        if (!insideTargetBlock &&
                (targetLineNumber == startLine || targetLineNumber == stopLine)) {
            if (isConditional(ctx)) {
                startTracking(ctx);
                matched = true;
            }
        }

        if (insideTargetBlock &&
                isConditional(ctx) &&
                isWithinMatchedBlock(ctx)) {
            System.out.println(ctx.getText());
            conditionalCount++;
        }

        if (insideTargetBlock && isWithinMatchedBlock(ctx)) {
            nestingLevel++;
        }
    }

    @Override
    public void exitStatement(JavaParser.StatementContext ctx) {
        int startLine = ctx.getStart().getLine();
        int stopLine = ctx.getStop().getLine();

        if (!matched &&
                startLine < targetLineNumber && targetLineNumber < stopLine &&
                isConditional(ctx)) {
            startTracking(ctx);
            matched = true;
        }

        if (insideTargetBlock &&
                isWithinMatchedBlock(ctx)) {
            nestingLevel--;
            if (nestingLevel == 0) {
                insideTargetBlock = false;
            }
        }
    }

    private boolean isConditional(JavaParser.StatementContext ctx) {
        return ctx.IF() != null || ctx.WHILE() != null || ctx.SWITCH() != null || ctx.FOR() != null;
    }

    private void startTracking(JavaParser.StatementContext ctx) {
        insideTargetBlock = true;
        conditionalCount = 0;
        nestingLevel = 1;

        blockStartIndex = ctx.getStart().getTokenIndex();
        blockStopIndex = ctx.getStop().getTokenIndex();

        Interval interval = new Interval(blockStartIndex, blockStopIndex);
        extractedBlock = tokens.getText(interval);
    }

    private boolean isWithinMatchedBlock(ParserRuleContext ctx) {
        int start = ctx.getStart().getTokenIndex();
        int stop = ctx.getStop().getTokenIndex();
        return start >= blockStartIndex && stop <= blockStopIndex;
    }

    public String getExtractedBlock() {
        return extractedBlock;
    }

    public int getConditionalCount() {
        return conditionalCount;
    }
}