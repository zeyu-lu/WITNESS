package witness_java;

import java.util.List;

public class LogEntry {
    private String mutationOperator;
    private List<String> diff;
    private String mutatedClass;
    private int lineNumber;
    private String str1;
    private String str2;

    // Constructor, getters and setters
    public LogEntry(String mutationOperator, List<String> diff, String mutatedClass, int lineNumber, String str1, String str2) {
        this.mutationOperator = mutationOperator;
        this.diff = diff;
        this.mutatedClass = mutatedClass;
        this.lineNumber = lineNumber;
        this.str1 = str1;
        this.str2 = str2;
    }

    // ... Getters and setters ...

    public String getMutationOperator() {
        return mutationOperator;
    }

    public void setMutationOperator(String mutationOperator) {
        this.mutationOperator = mutationOperator;
    }

    public List<String> getDiff() {
        return diff;
    }

    public void setDiff(List<String> diff) {
        this.diff = diff;
    }

    public String getMutatedClass() {
        return mutatedClass;
    }

    public void setMutatedClass(String mutatedClass) {
        this.mutatedClass = mutatedClass;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getStr1() {
        return str1;
    }

    public void setStr1(String str1) {
        this.str1 = str1;
    }

    public String getStr2() {
        return str2;
    }

    public void setStr2(String str2) {
        this.str2 = str2;
    }
}
