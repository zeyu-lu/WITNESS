package witness_java;

import java.util.Objects;

public class TestMutantPair implements Comparable<TestMutantPair> {
    public int testNo;
    public int mutantNo;

    public TestMutantPair(int testNo, int mutantNo) {
        this.testNo = testNo;
        this.mutantNo = mutantNo;
    }

    @Override
    public int hashCode() {
        return Objects.hash(testNo, mutantNo);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TestMutantPair that = (TestMutantPair) obj;
        return testNo == that.testNo && mutantNo == that.mutantNo;
    }

    @Override
    public int compareTo(TestMutantPair o) {
        if (this.testNo != o.testNo) {
            return Integer.compare(this.testNo, o.testNo);
        }
        return Integer.compare(this.mutantNo, o.mutantNo);
    }

    @Override
    public String toString() {
        return "TestNo: " + testNo + " , MutantNo: " + mutantNo;
    }
}