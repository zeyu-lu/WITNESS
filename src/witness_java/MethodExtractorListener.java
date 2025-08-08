package witness_java;

import myjava.JavaLexer;
import myjava.JavaParser;
import myjava.JavaParserBaseListener;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;

public class MethodExtractorListener extends JavaParserBaseListener {
    private JavaParser parser;
    private int targetLineNumber;
    private String extractedMethod;
    private boolean methodFound;
    private int methodStartLine;
    private int methodEndLine;

    public MethodExtractorListener(JavaParser parser, int targetLineNumber) {
        this.parser = parser;
        this.targetLineNumber = targetLineNumber;
        this.extractedMethod = "";
        this.methodFound = false;
    }

    @Override
    public void enterMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        handlePossibleMethodOrConstructor(ctx);
    }

    @Override
    public void enterConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
        handlePossibleMethodOrConstructor(ctx);
    }

    private void handlePossibleMethodOrConstructor(ParserRuleContext ctx) {
        if (methodFound) {
            return;
        }

        if (isLineNumberWithinRange(ctx)) {
            methodFound = true;
            extractedMethod = getMethodTextWithComments(ctx, targetLineNumber);
            methodStartLine = ctx.getStart().getLine();
            methodEndLine = ctx.getStop().getLine();
        }
    }

    private boolean isLineNumberWithinRange(ParserRuleContext ctx) {
        int startLine = ctx.getStart().getLine();
        int endLine = ctx.getStop().getLine();

        return targetLineNumber >= startLine && targetLineNumber <= endLine;
    }

    private String getMethodTextWithComments(ParserRuleContext ctx, int lineOfInterest) {
        TokenStream tokenStream = parser.getTokenStream();
        TokenStreamRewriter rewriter = new TokenStreamRewriter(tokenStream);

        int startTokenIndex = ctx.getParent().getParent() instanceof JavaParser.ClassBodyDeclarationContext ?
                ctx.getParent().getParent().start.getTokenIndex() :
                ctx.getStart().getTokenIndex();
        int stopTokenIndex = ctx.getStop().getTokenIndex();
        int startIndexWithComments = adjustStartIndexForComments(startTokenIndex, tokenStream);

        StringBuilder lineOfInterestContent = new StringBuilder();
        boolean lineOfInterestStarted = false;
        int firstTokenIndexOnLine = -1;
        int lastTokenIndexOnLine = -1;

        for (int i = startIndexWithComments; i <= stopTokenIndex; i++) {
            Token token = tokenStream.get(i);

            if (token.getLine() == lineOfInterest) {
                if (!lineOfInterestStarted) {
                    firstTokenIndexOnLine = i;
                    lineOfInterestContent.append(lineOfInterest).append(": ");
                    lineOfInterestStarted = true;
                }
                lastTokenIndexOnLine = i;
                lineOfInterestContent.append(token.getText());
            } else if (lineOfInterestStarted) {
                // Break out of the loop once the line of interest has ended
                break;
            }
        }

        if (firstTokenIndexOnLine != -1 && lastTokenIndexOnLine != -1) {
            // Insert <Before> and <After> in the rewriter
            rewriter.insertBefore(firstTokenIndexOnLine, "<Before> ");
            rewriter.insertAfter(lastTokenIndexOnLine, " <After>");
        }

        String modifiedMethodText = rewriter.getText(Interval.of(startIndexWithComments, stopTokenIndex));

        return modifiedMethodText;
    }

    private int adjustStartIndexForComments(int startTokenIndex, TokenStream tokenStream) {
        int index = startTokenIndex;

        while (index > 1) { // Changed to > 1 to ensure index - 2 does not go out of bounds
            Token token = tokenStream.get(index - 2); // Fetching token at index - 2

            // Checking if the token is a comment.
            if (isCommentToken(token)) {
                index -= 2; // Move index back to include this comment
            } else {
                // If the token is not a comment, stop the loop
                break;
            }
        }

        return index;
    }

    private boolean isCommentToken(Token token) {
        int type = token.getType();
        // Dynamically check if the token type corresponds to a comment.
        // Replace these with the actual token types for comments in your lexer.
        return type == JavaLexer.LINE_COMMENT || type == JavaLexer.COMMENT;
    }

    public boolean isMethodFound() {
        return methodFound;
    }

    public String getExtractedMethod() {
        return extractedMethod;
    }

}
