package witness_java;

public class SuffixMatcher {
    public String findCommonSuffix(String A, String B) {
        int aLength = A.length();
        int bLength = B.length();
        StringBuilder commonSuffix = new StringBuilder();

        // Start from the end of both strings and compare each character
        for (int i = 1; i <= Math.min(aLength, bLength); i++) {
            if (A.charAt(aLength - i) == B.charAt(bLength - i)) {
                commonSuffix.insert(0, A.charAt(aLength - i));
            } else {
                break;  // Stop as soon as characters don't match
            }
        }

        // Check if a common suffix was found
        if (commonSuffix.length() == 0) {
            return "-1";
        }
        return commonSuffix.toString();
    }

}
