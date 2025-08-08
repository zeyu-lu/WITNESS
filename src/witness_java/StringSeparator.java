package witness_java;

import java.util.ArrayList;
import java.util.List;

public class StringSeparator {

    public List<String> separateString(String str) {
        List<String> parts = new ArrayList<>();

        // Removing the leading and trailing brackets if present
        if (str.startsWith("[") && str.endsWith("]")) {
            str = str.substring(1, str.length() - 1);
        }

        int parenthesisCounter = 0;
        int separatorIndex = -1;

        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);

            if (ch == '(') {
                parenthesisCounter++;
            } else if (ch == ')') {
                parenthesisCounter--;
            } else if (ch == ':' && parenthesisCounter == 0) {
                separatorIndex = i;
                break;
            }
        }

        if (separatorIndex == -1) {
            parts.add(str); // Adding the whole string if no valid separator is found
            parts.add(""); // Adding an empty string as the second element
        } else {
            String originalPart = str.substring(0, separatorIndex);
            String updatePart = str.substring(separatorIndex + 1);

            parts.add(originalPart);
            parts.add(updatePart);
        }

        return parts;
    }
}
