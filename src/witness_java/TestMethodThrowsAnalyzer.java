package witness_java;

import myjava.JavaLexer;
import myjava.JavaParser;
import myjava.JavaParserBaseListener;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestMethodThrowsAnalyzer extends JavaParserBaseListener {

    Parser parser;
    BufferedTokenStream tokens;
    TokenStreamRewriter rewriter;

    String workdir;
    String srcTests;
    String testClass;
    List<String> testList;

    private List<String> packageList;
    private List<String> importList;

    private String extendCls;

    Map<String, Integer> testMap;

    MultiMap<String, String> paramMap;

    private boolean isExists = false;

    public TestMethodThrowsAnalyzer(Parser parser, BufferedTokenStream tokens, String workdir, String srcTests, String testClass, List<String> testList, Map<String, Integer> testMap, MultiMap<String, String> paramMap){
        this.parser = parser;
        this.tokens = tokens;
        rewriter = new TokenStreamRewriter(tokens);
        this.workdir = workdir;
        this.srcTests = srcTests;
        this.testClass = testClass;
        this.testList = testList;

        packageList = new ArrayList<>();
        importList = new ArrayList<>();
        this.testMap = testMap;
        this.paramMap = paramMap;
    }

    public boolean isExists() {
        return isExists;
    }

    @Override
    public void enterPackageDeclaration(JavaParser.PackageDeclarationContext ctx) {
        packageList.add(ctx.qualifiedName().getText());
    }

    @Override
    public void enterImportDeclaration(JavaParser.ImportDeclarationContext ctx) {
        if (ctx.MUL() != null){
            String path = ctx.qualifiedName().getText();
            path = path.replaceAll("\\.", "/");
            path = workdir + srcTests + "/" + path + "/";

            File folder = new File(path);
            if (!folder.exists()){
                return;
            }
            File[] listOfFiles = folder.listFiles();

            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    String fileName = listOfFiles[i].getName();
                    String[] arr = fileName.split("\\.");
                    importList.add(ctx.qualifiedName().getText() + "." + arr[0]);
                }
            }
        }else{
            importList.add(ctx.qualifiedName().getText());
        }

    }

    @Override
    public void enterClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        if (ctx.EXTENDS() != null) {
            extendCls = ctx.typeType().classOrInterfaceType().typeIdentifier().getText();

            boolean flag = false;
            for (String importDecl : importList) {
                String[] arr = importDecl.split("\\.");
                if (arr[arr.length - 1].equals(extendCls)) {
                    flag = true;

                    String classPath = importDecl.replaceAll("\\.", "/");
                    File file = new File(workdir + srcTests + "/" + classPath + ".java");
                    if (!file.exists()) {
                        return;
                    }
                    String code;
                    try {
                        code = new String(Files.readAllBytes(file.toPath()), Charset.forName("UTF-8"));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    JavaLexer lexer = new JavaLexer(new ANTLRInputStream(code));
                    CommonTokenStream tokens = new CommonTokenStream(lexer);
                    JavaParser parser = new JavaParser(tokens);

                    ParseTree tree = parser.compilationUnit();
                    ParseTreeWalker walker = new ParseTreeWalker();
                    AssertionCounterListener assertionCounterListener = new AssertionCounterListener(parser, tokens, workdir, srcTests, testClass, testList, testMap, paramMap);
                    walker.walk(assertionCounterListener, tree);

                }
            }  // for

            if (!flag) {

                for (String packageDecl : packageList) {

                    String classPath = packageDecl.replaceAll("\\.", "/");
                    File file = new File(workdir + srcTests + "/" + classPath + "/" + extendCls + ".java");
                    if (!file.exists()) {
                        return;
                    }
                    String code;
                    try {
                        code = new String(Files.readAllBytes(file.toPath()), Charset.forName("UTF-8"));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    JavaLexer lexer = new JavaLexer(new ANTLRInputStream(code));
                    CommonTokenStream tokens = new CommonTokenStream(lexer);
                    JavaParser parser = new JavaParser(tokens);

                    ParseTree tree = parser.compilationUnit();
                    ParseTreeWalker walker = new ParseTreeWalker();
                    AssertionCounterListener assertionCounterListener = new AssertionCounterListener(parser, tokens, workdir, srcTests, testClass, testList, testMap, paramMap);
                    walker.walk(assertionCounterListener, tree);

                }  // for

            }
        }

    }

    @Override
    public void enterClassBodyDeclaration(JavaParser.ClassBodyDeclarationContext ctx) {
        if (ctx.memberDeclaration() != null){
            if (ctx.memberDeclaration().methodDeclaration() != null) {

                String testName = ctx.memberDeclaration().methodDeclaration().identifier().getText();
                testName = "[" + testName + "]";

                if (testList.contains(testName) ) {
                    testMap.replace(testName, testMap.get(testName) + 1);

                    if (ctx.memberDeclaration().methodDeclaration().THROWS() != null) {
                        isExists = true;
                    }
                } else {
                    if (paramMap.containsKey(testName)){
                        List<String> paramList = (List<String>)paramMap.get(testName);
                        for (String paramTest : paramList) {
                            testName = paramTest;
                            testMap.replace(testName, testMap.get(testName) + 1);

                            if (ctx.memberDeclaration().methodDeclaration().THROWS() != null) {
                                isExists = true;
                            }
                        }
                    }
                }
            }
        }

    }

}
