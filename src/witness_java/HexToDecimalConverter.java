package witness_java;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HexToDecimalConverter {

    public String convertHexToDecimal(String input) {
        Pattern hexPattern = Pattern.compile("0x[0-9a-fA-F]+");
        Matcher matcher = hexPattern.matcher(input);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String hexString = matcher.group();
            int decimalValue = Integer.parseInt(hexString.substring(2), 16);
            matcher.appendReplacement(result, String.valueOf(decimalValue));
        }
        matcher.appendTail(result);

        return result.toString();
    }

}