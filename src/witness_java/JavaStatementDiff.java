package witness_java;

public class JavaStatementDiff {

    public String getDifference(String original, String updated) {
        int minLength = Math.min(original.length(), updated.length());

        // Find the index at which the difference starts
        int diffIndex = -1;
        for (int i = 0; i < minLength; i++) {
            if (original.charAt(i) != updated.charAt(i)) {
                diffIndex = i;
                break;
            }
        }

        // If no difference is found and strings are of different lengths
        if (diffIndex == -1 && original.length() != updated.length()) {
            diffIndex = minLength;
        }

        // If no difference is found at all
        if (diffIndex == -1) {
            return "No difference";
        }

        // Extract the differing parts from the original and updated strings
        String originalDiff = extractDiffPart(original, diffIndex);
        String updatedDiff = extractDiffPart(updated, diffIndex);

        return "['" + originalDiff + "', '" + updatedDiff + "']";
    }

    private String extractDiffPart(String str, int diffIndex) {
        int start = diffIndex;

        // Move backwards to include the entire operator if it's multi-character
        while (start > 0 && !Character.isWhitespace(str.charAt(start - 1))) {
            start--;
        }

        int end;
        if ((start+1) < str.length() && isOperatorBoundary(str.charAt(start+1))){
            end = start + 1;
        } else {
            end = diffIndex + 1;
            // Move forwards to include the entire operator if it's multi-character
            while (end < str.length() && !Character.isWhitespace(str.charAt(end)) && !isOperatorBoundary(str.charAt(end))) {
                end++;
            }
        }

        return str.substring(start, end);
    }

    private boolean isOperatorBoundary(char c) {
        // Add other operator boundary characters as needed
        return c == '(' || c == ')' || c == ';' || c == '}';
    }

}
