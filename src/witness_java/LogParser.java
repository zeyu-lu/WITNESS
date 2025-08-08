package witness_java;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogParser {

    public Map<Integer, LogEntry> parseLogFile(String filePath) throws IOException {
        Map<Integer, LogEntry> entries = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length < 6) continue; // Ensure there are enough parts

                int mutantNo = Integer.parseInt(parts[0]);
                String mutationOperator = parts[1];

                // Find and extract LineNumber, which is purely numeric and the second last part
                int lineNumberIndex = -1;
                for (int i = parts.length - 2; i >= 0; i--) {
                    if (parts[i].matches("\\d+")) {
                        lineNumberIndex = i;
                        break;
                    }
                }
                if (lineNumberIndex == -1) continue; // Skip if LineNumber is not found

                int lineNumber = Integer.parseInt(parts[lineNumberIndex]);

                // Creating a string from the remaining parts
                StringBuilder remainingString = new StringBuilder();
                for (int i = lineNumberIndex + 1; i < parts.length; i++) {
                    remainingString.append(parts[i]);
                    if (i < parts.length - 1) {
                        remainingString.append(" ");
                    }
                }

                // Splitting the remaining string by "|==>"
                String[] splitParts = remainingString.toString().split("\\|==>");
                String str1 = splitParts.length > 0 ? splitParts[0].trim() : "";
                String str2 = splitParts.length > 1 ? splitParts[1].trim() : "";
                if (str2.equals("<NO-OP>")){
                    str2 = "";
                }

                // Extract MutatedClass, removing part after "@" if present
                String mutatedClass = parts[lineNumberIndex - 1].split("@")[0];

                // Reconstruct the Diff part
                StringBuilder diffBuilder = new StringBuilder();
                for (int i = 2; i < lineNumberIndex - 1; i++) {
                    if (i > 2) diffBuilder.append(":");
                    diffBuilder.append(parts[i]);
                }
                List<String> diff = Collections.singletonList(diffBuilder.toString());

                entries.put(mutantNo, new LogEntry(mutationOperator, diff, mutatedClass, lineNumber, str1, str2));
            }
        }
        return entries;
    }

}
