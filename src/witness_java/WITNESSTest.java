package witness_java;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WITNESSTest {

    private String workdir;

    public WITNESSTest(String workdir) {
        this.workdir = workdir;
    }

    public static void main(String[] args) throws Exception {
        List<String> workdirs = new ArrayList<>();
        workdirs.add("../commons-csv/csv_1_fixed/");

        for (String workdir : workdirs) {
            String testMapFile = workdir + "testMap.csv";

            TestMapReader testMapReader = new TestMapReader();
            Map<Integer, String> testMap = testMapReader.readTestMapFromCSV(testMapFile);

            for (Map.Entry<Integer, String> entry : testMap.entrySet()) {
                Integer testNo = entry.getKey();
                String testName = entry.getValue();

                // Output the extracted columns
                System.out.println("TestNo: " + testNo);
                System.out.println("TestName: " + testName);

                //
                WITNESSTest witnessTest = new WITNESSTest(workdir);
                witnessTest.myPair(testNo, testName);
            }
        }

    }

    public void myPair(int testNo, String testName) throws Exception {

        TestCaseExtractor testCaseExtractor = new TestCaseExtractor();
        List<Integer> testCaseMetrics = testCaseExtractor.extractor(workdir, testName);
        int linesInTestCase = testCaseMetrics.get(0);
        int assertionNumber = testCaseMetrics.get(1);

        boolean isExists = testCaseExtractor.boolExtractor(workdir, testName);
        System.out.println("throws: " + isExists);
        String hasThrow = "";
        if (isExists) {
            hasThrow = "THROWS";
        }

        String output = testNo + ";" + linesInTestCase + ";" + assertionNumber + ";" + hasThrow + "\n";
        String fileName = workdir + "witness_feature_test.txt";
        BufferedWriter f_writer = new BufferedWriter(new FileWriter(fileName, true));
        f_writer.write(output);
        f_writer.close();

    }

}
