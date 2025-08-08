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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class JavaMethodFinder extends JavaParserBaseListener {
    private String methodName = null;
    private List<String> parameterTypes = new ArrayList<>();
    private String outermostClassName = null;
    private String returnType = null;
    private int lineOfInterest;
    private int relativeLineNumber = -1; // Initialized to -1 to indicate it's not set
    private double relativePosition = 0.0;

    public JavaMethodFinder(int lineOfInterest) {
        this.lineOfInterest = lineOfInterest;
    }

    @Override
    public void enterClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        // If outermostClassName is not set, this is the outermost class
        if (outermostClassName == null) {
            outermostClassName = ctx.identifier().IDENTIFIER().getText();
        }
    }

    @Override
    public void enterMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        if (ctx.start.getLine() <= lineOfInterest && ctx.stop.getLine() >= lineOfInterest) {
            if (ctx.identifier().IDENTIFIER() != null) {
                methodName = ctx.identifier().IDENTIFIER().getText();
            } else {
                methodName = ctx.identifier().getText();
            }
            returnType = ctx.typeTypeOrVoid().getText();

            // Calculate the relative line number
            relativeLineNumber = lineOfInterest - ctx.start.getLine();
            relativePosition = 1.0 * (lineOfInterest - ctx.start.getLine()) / (ctx.stop.getLine() - ctx.start.getLine());

            JavaParser.FormalParameterListContext paramsCtx = ctx.formalParameters().formalParameterList();
            if (paramsCtx != null) {
                for (JavaParser.FormalParameterContext paramCtx : paramsCtx.formalParameter()) {
                    parameterTypes.add(paramCtx.typeType().getText());
                }
            }
        }
    }

    @Override
    public void enterGenericMethodDeclaration(JavaParser.GenericMethodDeclarationContext ctx) {
        enterMethodDeclaration(ctx.methodDeclaration());
    }

    @Override
    public void enterConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
        if (ctx.start.getLine() <= lineOfInterest && ctx.stop.getLine() >= lineOfInterest) {
            methodName = ctx.identifier().IDENTIFIER().getText();

            // Calculate the relative line number
            relativeLineNumber = lineOfInterest - ctx.start.getLine();
            relativePosition = 1.0 * (lineOfInterest - ctx.start.getLine()) / (ctx.stop.getLine() - ctx.start.getLine());

            JavaParser.FormalParameterListContext paramsCtx = ctx.formalParameters().formalParameterList();
            if (paramsCtx != null) {
                for (JavaParser.FormalParameterContext paramCtx : paramsCtx.formalParameter()) {
                    parameterTypes.add(paramCtx.typeType().getText());
                }
            }
        }
    }

    @Override
    public void enterGenericConstructorDeclaration(JavaParser.GenericConstructorDeclarationContext ctx) {
        enterConstructorDeclaration(ctx.constructorDeclaration());
    }

    public String getMethodName() {
        return methodName;
    }

    public List<String> getParameterTypes() {
        return parameterTypes;
    }

}
