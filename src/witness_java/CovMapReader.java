package witness_java;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CovMapReader {

    public List<TestMutantPair> readCsvFile(String csvFile) {
        List<TestMutantPair> pairs = new ArrayList<>();
        String line;
        String csvSplitBy = ",";

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            if (br.readLine() != null) { // Skip the header line
                while ((line = br.readLine()) != null) {
                    String[] data = line.split(csvSplitBy);
                    int testNo = Integer.parseInt(data[0]);
                    int mutantNo = Integer.parseInt(data[1]);
                    pairs.add(new TestMutantPair(testNo, mutantNo));
                }
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }

        return pairs;
    }
}
