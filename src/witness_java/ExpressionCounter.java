package witness_java;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExpressionCounter {
    
    public int countSubExpressions(String expression) {
        if (expression == null || expression.isEmpty()) {
            return 0;
        }
        
        // Use regex to find occurrences of && and ||
        Pattern pattern = Pattern.compile("&&|\\|\\|");
        Matcher matcher = pattern.matcher(expression);
        
        int count = 1; // Start with 1 because sub-expressions are delimited by operators
        while (matcher.find()) {
            count++;
        }
        
        return count;
    }

}
