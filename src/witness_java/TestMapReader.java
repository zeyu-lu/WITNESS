package witness_java;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TestMapReader {

    public static Map<Integer, String> readTestMapFromCSV(String testMapFile) {
        Map<Integer, String> testMap = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(testMapFile))) {
            String line;

            // Skip the first line (header)
            br.readLine();

            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                // Convert the first value to Integer before using it as a key
                Integer testNo = Integer.parseInt(values[0]);
                testMap.put(testNo, values[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return testMap;
    }

}
