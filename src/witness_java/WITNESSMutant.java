package witness_java;

import myjava.JavaLexer;
import myjava.JavaParser;
import myjava.JavaParserBaseListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.*;
import java.util.*;

import static myfpmt.ConditionalSkeletonExtractor.replaceInSpecificLine;

public class WITNESSMutant {

    private String workdir;

    public WITNESSMutant(String workdir) {
        this.workdir = workdir;
    }

    public static void main(String[] args) throws Exception {
        List<String> workdirs = new ArrayList<>();
        workdirs.add("../commons-csv/csv_1_fixed/");

        for (String workdir : workdirs) {
            String covMapFile = workdir + "covMap.csv";
            String logFile = workdir + "mutants.log";

            CovMapReader covMapReader = new CovMapReader();
            List<TestMutantPair> covMapPairs = covMapReader.readCsvFile(covMapFile);

            Set<Integer> mutantNoSet = new HashSet<>();

            for (TestMutantPair pair : covMapPairs) {
                int mutantNo = pair.mutantNo;
                mutantNoSet.add(mutantNo);
            }

            for (int mutantNo : mutantNoSet) {
                // Your code here
                System.out.println("Mutant number: " + mutantNo);

                LogParser logParser = new LogParser();
                Map<Integer, LogEntry> entries = logParser.parseLogFile(logFile);
                LogEntry logEntry = entries.get(mutantNo);
                String mutatedClass = logEntry.getMutatedClass();
                String mutationOperator = logEntry.getMutationOperator();
                String diff = logEntry.getDiff().toString();
                int lineNumber = logEntry.getLineNumber();
                String str1 = logEntry.getStr1();
                String str2 = logEntry.getStr2();

                //
                WITNESSMutant witnessMutant = new WITNESSMutant(workdir);
                witnessMutant.myPair(mutantNo, mutatedClass, mutationOperator,
                        diff, lineNumber, str1, str2);
            }
        }

    }

    public void myPair(int mutantNo, String mutatedClass, String mutationOperator,
                       String diff, int lineNumber, String str1, String str2) throws Exception {
        int dollarIndex = mutatedClass.indexOf("$");
        String processedString;
        if (dollarIndex != -1){
            processedString = mutatedClass.substring(0, dollarIndex);
        }else{
            processedString = mutatedClass;
        }
        processedString = processedString.replaceAll("\\.", "/");

        String srcClasses = getSrcClasses();
        String filePath = workdir + srcClasses + "/" + processedString + ".java";

        CharStream codeCharStream = CharStreams.fromFileName(filePath);
        JavaLexer lexer = new JavaLexer(codeCharStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JavaParser parser = new JavaParser(tokens);
//
        ParseTree tree = parser.compilationUnit();
//
        String myType = statementType(lineNumber, tokens, tree);
//
        int nestingLevel = conditionalNestingLevel(lineNumber, tree);
//
        int counter = usageCounter(lineNumber, tokens, tree);
//
        int linesInMethod = countLinesInMethod(lineNumber, parser, tree);
//
        int sourceComplexity = mcCabeComplexityCalculator(mutatedClass, lineNumber, tree);
//        int complexity = 0;
//
        List<Integer> integerList = callCallby(mutatedClass, lineNumber, tree);
        int call = integerList.get(0);
        int callby = integerList.get(1);
//        int call = 0;
//        int callby = 0;
//
        List<String> variableAnalyzerList = variableAnalyzer(lineNumber, tree);
        String declaredVariableType = variableAnalyzerList.get(0);
        String variableIsFinalNew = variableAnalyzerList.get(1);
//        String stringType = variableAnalyzer(lineNumber, tree);
//
        String parentType = parentContextType(lineNumber, tokens, tree);
//
        List<Integer> conditionalBlockList = conditionalBlock(lineNumber, tokens, tree);
        int conditionalBlockLOC =  conditionalBlockList.get(0);
        int conditionalBlockCount = conditionalBlockList.get(1);
//
        String detected = hasReturnOrThrow(lineNumber, tokens, parser, tree);
//
        String myDiff = statementDiff(mutationOperator, diff, myType);
//
        List<String> skeletonList = conditionalSkeletonChange(codeCharStream, lineNumber, tokens, tree, str1, str2);
        String skeletonModification = skeletonList.toString();
//
        String output = mutantNo + ";" + nestingLevel + ";" + myType + ";" + counter + ";" + linesInMethod + ";"
                + sourceComplexity + ";" + call + ";" + callby + ";" + declaredVariableType + ";" + variableIsFinalNew + ";"
                + parentType + ";" + conditionalBlockLOC + ";" + conditionalBlockCount + ";" + detected + ";"
                + mutationOperator + ";" + myDiff + ";" + skeletonModification + "\n";
        String fileName = workdir + "witness_feature_mutant.txt";
        BufferedWriter f_writer = new BufferedWriter(new FileWriter(fileName, true));
        f_writer.write(output);
        System.out.println("output: " + output);
        f_writer.close();

    }

    public String getSrcClasses() throws IOException, InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        Process process = null;

        String command = "cat defects4j.build.properties";

        process = runtime.exec(command, null, new File(workdir));

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String srcTests = null;

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains("d4j.dir.src.classes")) {
                line = line.trim();
                int idx1 = line.indexOf("=");
                srcTests = line.substring(idx1 + 1, line.length());
            }
        }

        process.waitFor();
        return srcTests;
    }

    public String statementType(int lineNumber, CommonTokenStream tokens, ParseTree tree) {
        // Create the listener
        JavaStatementTypeListener listener = new JavaStatementTypeListener(lineNumber, tokens);

        // Walk the parse tree
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(listener, tree);

        String prefix = "";

        String myType = listener.getType();
        if (!myType.equals("Unknown")) {
            return prefix + myType;
        } else {
            StatementTypeIdentifier statementTypeIdentifier = new StatementTypeIdentifier(lineNumber, tokens);
            walker.walk(statementTypeIdentifier, tree);
            return prefix + statementTypeIdentifier.getType();
        }
    }

    public int conditionalNestingLevel(int lineNumber, ParseTree tree) {
        CustomListener listener = new CustomListener(lineNumber);
        ParseTreeWalker.DEFAULT.walk(listener, tree);

        int nestingLevel = listener.getResult();

        System.out.println("Nesting Level at line " + lineNumber + ": " + nestingLevel);

        return nestingLevel;
    }

    public int usageCounter(int lineOfInterest, CommonTokenStream tokens, ParseTree tree) {
        UsageCounterListener listener = new UsageCounterListener(lineOfInterest, tokens);
        // Walk the parse tree
        ParseTreeWalker.DEFAULT.walk(listener, tree);

        int total = 0;
        for (Map.Entry<String, Integer> entry : listener.usageCount.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
            total += entry.getValue();
        }
        return total;
    }

    public int countLinesInMethod(int targetLineNumber, JavaParser parser, ParseTree tree) throws IOException {
        MethodExtractorListener extractor = new MethodExtractorListener(parser, targetLineNumber);
        ParseTreeWalker.DEFAULT.walk(extractor, tree);

        int count = 0;

        if (extractor.isMethodFound()) {
            System.out.println(extractor.getExtractedMethod());

            SourceCodeLineCounter sourceCodeLineCounter = new SourceCodeLineCounter();
            String test = extractor.getExtractedMethod();
            Reader inputString = new StringReader(test);
            BufferedReader reader = new BufferedReader(inputString);
            count = sourceCodeLineCounter.getNumberOfLines(reader);
            System.out.println("count: " + count);
        }

        return count;
    }

    public int mcCabeComplexityCalculator(String mutatedClass, int lineNumber, ParseTree tree) throws IOException, InterruptedException {
        JavaMethodFinder finder = new JavaMethodFinder(lineNumber);
        ParseTreeWalker.DEFAULT.walk(finder, tree);

        int complexity = 0;
        if (finder.getMethodName() != null) {

            String path = workdir;
            path = path.substring(0, path.length() - 1);
            int lastSlashIndex = path.lastIndexOf('/');
            String lastPart = path.substring(lastSlashIndex + 1);
            String projectPath = workdir + lastPart + ".udb";

            int dollarIndex = mutatedClass.indexOf("$");
            String className;
            if (dollarIndex != -1){
                className = mutatedClass.substring(0, dollarIndex);
            }else{
                className = mutatedClass;
            }

            String methodName = finder.getMethodName();

            String parameterTypesInput = null;
            if (finder.getParameterTypes() != null) {
                List<String> parameterTypes = finder.getParameterTypes();

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < parameterTypes.size(); i++) {
                    sb.append(parameterTypes.get(i));
                    if (i < parameterTypes.size() - 1) {
                        sb.append(",");
                    }
                }

                parameterTypesInput = sb.toString();
            }

            // Starting the Python script with arguments using ProcessBuilder
            ProcessBuilder builder;
            if (!parameterTypesInput.isEmpty()) {
                builder = new ProcessBuilder("python3", "../my_understand_mccabe_cli.py", projectPath, className, methodName, "--parameter_types", parameterTypesInput);
            }else{
                builder = new ProcessBuilder("python3", "../my_understand_mccabe_cli.py", projectPath, className, methodName);
            }
            Process process = builder.start();

            // Reading the output from the Python script
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("McCabe Complexity of method")){
                    // Splitting the string by "is:"
                    String[] parts = line.split("is:");
                    complexity = Integer.parseInt(parts[1].trim());
                }
            }

            // Waiting for the process to finish
            int exitCode = process.waitFor();
            System.out.println("Python script exited with code " + exitCode);

        }

        return complexity;
    }

    public List<Integer> callCallby(String mutatedClass, int lineNumber, ParseTree tree) throws IOException, InterruptedException {
        JavaMethodFinder finder = new JavaMethodFinder(lineNumber);
        ParseTreeWalker.DEFAULT.walk(finder, tree);

        List<Integer> list = new ArrayList<>();

        if (finder.getMethodName() != null) {

            String path = workdir;
            path = path.substring(0, path.length() - 1);
            int lastSlashIndex = path.lastIndexOf('/');
            String lastPart = path.substring(lastSlashIndex + 1);
            String projectPath = workdir + lastPart + ".udb";

            int dollarIndex = mutatedClass.indexOf("$");
            String className;
            if (dollarIndex != -1){
                className = mutatedClass.substring(0, dollarIndex);
            }else{
                className = mutatedClass;
            }

            String methodName = finder.getMethodName();
            System.out.println("Method Name: " + methodName);

            // Starting the Python script with arguments using ProcessBuilder
            ProcessBuilder builder;
            builder = new ProcessBuilder("python3", "../my_understand_call_callby_cli.py", projectPath, className, methodName);
            Process process = builder.start();

            // Reading the output from the Python script
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Methods Calls")){
                    // Splitting the string by "is:"
                    String[] parts = line.split(":");
                    int calls = Integer.parseInt(parts[1].trim());
                    list.add(calls);
                }
                if (line.startsWith("Methods Called By")){
                    // Splitting the string by "is:"
                    String[] parts = line.split(":");
                    int callby = Integer.parseInt(parts[1].trim());
                    list.add(callby);
                }
            }

            // Waiting for the process to finish
            int exitCode = process.waitFor();
            System.out.println("Python script exited with code " + exitCode);

        } else {
            list.add(0);
            list.add(0);
        }

        return list;
    }

    public List<String> variableAnalyzer(int lineNumber, ParseTree tree) {
        MemberVariableAnalyzer listener = new MemberVariableAnalyzer(lineNumber);

        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(listener, tree);

        List<String> list = new ArrayList<>();
        list.add(listener.getType());

        if (listener.getIsFinal().isEmpty() && listener.getHasNew().isEmpty()) {
            list.add("");
        }else if (!listener.getIsFinal().isEmpty() && listener.getHasNew().isEmpty()) {
            list.add(listener.getIsFinal());
        }else if (listener.getIsFinal().isEmpty() && !listener.getHasNew().isEmpty()) {
            list.add(listener.getHasNew());
        }else {
            list.add(listener.getIsFinal() + "-" + listener.getHasNew());
        }

        return list;
    }

    public String parentContextType(int lineNumber, CommonTokenStream tokens, ParseTree tree) {
        JavaParentContextFinder listener = new JavaParentContextFinder(tokens, lineNumber);
        ParseTreeWalker.DEFAULT.walk(listener, tree);

        if (listener.getParentContextType() != null) {
            System.out.println("Parent Context Type: " + listener.getParentContextType());
            return listener.getParentContextType();
        } else {
            System.out.println("No RETURN or THROW found on line " + lineNumber);
            return "";
        }
    }

    public List<Integer> conditionalBlock(int lineNumber, CommonTokenStream tokens, ParseTree tree) throws IOException {
        ConditionalListener listener = new ConditionalListener(lineNumber, tokens);
        ParseTreeWalker.DEFAULT.walk(listener, tree);

        String block = listener.getExtractedBlock();
        int count = listener.getConditionalCount();

        List<Integer> list = new ArrayList<>();

        if (block != null) {
            Reader inputString = new StringReader(block);
            BufferedReader reader = new BufferedReader(inputString);
            SourceCodeLineCounter sourceCodeLineCounter = new SourceCodeLineCounter();
            int linesOfCode = 0;
            linesOfCode = sourceCodeLineCounter.getNumberOfLines(reader);

            list.add(linesOfCode);
            list.add(count);
        } else {
            System.out.println("No conditional block found at line " + lineNumber);
            list.add(0);
            list.add(0);
        }

        return list;
    }

    public String hasReturnOrThrow(int lineNumber, CommonTokenStream tokens, JavaParser parser, ParseTree tree) {
        ConditionalBlockAnalyzer listener = new ConditionalBlockAnalyzer(lineNumber, tokens);
        listener.setParser(parser);

        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(listener, tree);

        String result = listener.getReturnOrThrow();
        if (!result.isEmpty()) {
            System.out.println("Detected: " + result);
            return result;
        } else {
            ReturnThrowVariableExtractor extractor = new ReturnThrowVariableExtractor();
            JavaParserBaseListener returnThrowVariableListener =
                    extractor.new StatementListener(extractor.getResult());
            ParseTreeWalker.DEFAULT.walk(returnThrowVariableListener, tree);

            Set<String> assigned = listener.getAssignedVariables();
            Set<String> returned = extractor.getResult().get("RETURN");
            Set<String> thrown = extractor.getResult().get("THROW");

            boolean isReturned = false;
            boolean isThrown = false;

            for (String var : assigned) {
                if (returned.contains(var)) {
                    isReturned = true;
                }
                if (thrown.contains(var)) {
                    isThrown = true;
                }
            }

            String returnOrThrow = "";
            if (isReturned && isThrown) {
                returnOrThrow = "RETURN-THROW";
            } else if (isReturned) {
                returnOrThrow = "RETURN";
            } else if (isThrown) {
                returnOrThrow = "THROW";
            }

            return returnOrThrow;
        }
    }

    public String statementDiff(String mutationOperator, String diff, String type) {
        diff = diff.substring(1, diff.length() - 1);

        StringSeparator stringSeparator = new StringSeparator();
        List<String> parts = stringSeparator.separateString(diff);
        String original = parts.get(0);
        String updated = parts.get(1);

        if (mutationOperator.equals("COR")) {
            SuffixMatcher suffixMatcher = new SuffixMatcher();
            String commonSuffix = suffixMatcher.findCommonSuffix(original, updated);
            if ((updated.equals("TRUE") || updated.equals("FALSE")) && commonSuffix.equals("-1")) {
                original = type;
            }
        }

        JavaStatementDiff javaStatementDiff = new JavaStatementDiff();
        String myDiff = javaStatementDiff.getDifference(original, updated);
        return myDiff;

    }

    public List<String> conditionalSkeletonChange(CharStream charStream, int lineNumber, CommonTokenStream tokens, ParseTree tree, String str1, String str2){
        ParseTreeWalker walker = new ParseTreeWalker();
        ConditionalSkeletonExtractor extractor = new ConditionalSkeletonExtractor(charStream, lineNumber);
        walker.walk(extractor, tree);

        List<String> skeletonList = new ArrayList<>();
        System.out.println("extractor.isConditionFound(): " + extractor.isConditionFound());

        // Check if the condition is found
        if (extractor.isConditionFound()) {
            // Define the original and updated conditions
            String originalCondition = extractor.getStoredCondition();

            boolean isEqual = false;
            ExpressionCounter counter = new ExpressionCounter();
            int result1 = counter.countSubExpressions(str1);
            int result2 = counter.countSubExpressions(str2);
            if (result1 == result2){
                isEqual = true;
            }

            String originalSkeleton = extractor.transformToSkeleton(originalCondition, isEqual);
            skeletonList.add(originalSkeleton);

            int line = extractor.getConditionStartLine();
            String updatedCondition = replaceInSpecificLine(originalCondition, str1, str2, line, lineNumber);

            // Modify extractor for rewriting
            extractor = new ConditionalSkeletonExtractor(charStream, lineNumber, tokens, originalCondition, updatedCondition);
            walker.walk(extractor, tree);

            // Get the modified code after rewriting
            String modifiedCode = extractor.getModifiedCode();

            // Second pass: re-parse the modified code and extract the updated condition skeleton
            CharStream modifiedCharStream = CharStreams.fromString(modifiedCode);
            JavaLexer modifiedLexer = new JavaLexer(modifiedCharStream);
            CommonTokenStream modifiedTokens = new CommonTokenStream(modifiedLexer);
            JavaParser modifiedParser = new JavaParser(modifiedTokens);

            tree = modifiedParser.compilationUnit();
            ConditionalSkeletonExtractor modifiedExtractor = new ConditionalSkeletonExtractor(modifiedCharStream, lineNumber);
            walker.walk(modifiedExtractor, tree);

            String modifiedSkeleton = modifiedExtractor.transformToSkeleton(modifiedExtractor.getStoredCondition(), isEqual);
            skeletonList.add(modifiedSkeleton);
        }

        return skeletonList;
    }

}
