package witness_java;

import myjava.JavaLexer;
import myjava.JavaParser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;

public class TestCaseExtractor {

    public String getSrcTests(String workdir) throws IOException, InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        Process process = null;

        String command = "cat defects4j.build.properties";

        process = runtime.exec(command, null, new File(workdir));

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String srcTests = null;

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains("d4j.dir.src.tests")) {
                line = line.trim();
                int idx1 = line.indexOf("=");
                srcTests = line.substring(idx1 + 1, line.length());
            }
        }

        process.waitFor();
        return srcTests;
    }

    public List<Integer> extractor(String workdir, String testCase) throws IOException, InterruptedException {
        MultiMap<String, String> multiMap = new MultiMap<>();
        int index = testCase.indexOf("[");
        multiMap.put(testCase.substring(0, index), testCase.substring(index, testCase.length()));

        String srcTests = getSrcTests(workdir);

        List<Integer> list = new ArrayList<>();
        int loc = 0;
        int assertNum = 0;

        for (Map.Entry<String, Collection<String>> entry : multiMap.entrySet()) {
            String key = entry.getKey();
            List<String> testList = (List<String>) multiMap.get(key);

            Map<String, Integer> testMap = new HashMap<>();
            for (String test : testList) {
                testMap.put(test, 0);
            }

            MultiMap<String, String> paramMap = new MultiMap<>();
            for (String test : testList) {
                if (test.contains("]]")) {
                    int idx = test.lastIndexOf("[");
                    paramMap.put(test.substring(0, idx) + "]", test);
                }
            }

            String classPath = key.replaceAll("\\.", "/");
            if (classPath.contains("$")) {
                int endIndex = classPath.indexOf("$");
                classPath = classPath.substring(0, endIndex);
            }
            File file = new File(workdir + srcTests + "/" + classPath + ".java");
            String code = new String(Files.readAllBytes(file.toPath()), Charset.forName("UTF-8"));
            JavaLexer lexer = new JavaLexer(new ANTLRInputStream(code));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            JavaParser parser = new JavaParser(tokens);

            ParseTree tree = parser.compilationUnit();
            ParseTreeWalker walker = new ParseTreeWalker();
            //
            TestCaseLOCListener testCaseLOCListener = new TestCaseLOCListener(parser, tokens, workdir, srcTests, key, testList, testMap, paramMap);
            walker.walk(testCaseLOCListener, tree);

            loc = testCaseLOCListener.getLOC();
            //
            AssertionCounterListener assertionCounterListener = new AssertionCounterListener(parser, tokens, workdir, srcTests, key, testList, testMap, paramMap);
            walker.walk(assertionCounterListener, tree);
            assertNum = assertionCounterListener.getAssertNum();
            //
        }

        list.add(loc);
        list.add(assertNum);
        return list;
    }

    public Boolean boolExtractor(String workdir, String testCase) throws IOException, InterruptedException {
        MultiMap<String, String> multiMap = new MultiMap<>();
        int index = testCase.indexOf("[");
        multiMap.put(testCase.substring(0, index), testCase.substring(index, testCase.length()));

        String srcTests = getSrcTests(workdir);

        boolean isExists = false;

        for (Map.Entry<String, Collection<String>> entry : multiMap.entrySet()) {
            String key = entry.getKey();
            List<String> testList = (List<String>) multiMap.get(key);

            Map<String, Integer> testMap = new HashMap<>();
            for (String test : testList) {
                testMap.put(test, 0);
            }

            MultiMap<String, String> paramMap = new MultiMap<>();
            for (String test : testList) {
                if (test.contains("]]")) {
                    int idx = test.lastIndexOf("[");
                    paramMap.put(test.substring(0, idx) + "]", test);
                }
            }

            String classPath = key.replaceAll("\\.", "/");
            if (classPath.contains("$")) {
                int endIndex = classPath.indexOf("$");
                classPath = classPath.substring(0, endIndex);
            }
            File file = new File(workdir + srcTests + "/" + classPath + ".java");
            String code = new String(Files.readAllBytes(file.toPath()), Charset.forName("UTF-8"));
            JavaLexer lexer = new JavaLexer(new ANTLRInputStream(code));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            JavaParser parser = new JavaParser(tokens);

            ParseTree tree = parser.compilationUnit();
            ParseTreeWalker walker = new ParseTreeWalker();

            TestMethodThrowsAnalyzer testMethodThrowsAnalyzer = new TestMethodThrowsAnalyzer(parser, tokens, workdir, srcTests, key, testList, testMap, paramMap);
            walker.walk(testMethodThrowsAnalyzer, tree);
            isExists = testMethodThrowsAnalyzer.isExists();
        }

        return isExists;
    }

}
