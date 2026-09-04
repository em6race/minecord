
import java.util.regex.*;

public class TestRegex {
    public static void main(String[] args) {
        String msg = "doks was slain by Spider";
        Pattern pattern = Pattern.compile("^(.*?) was slain by (.*?)$");
        Matcher m = pattern.matcher(msg);
        if (m.find()) {
            System.out.println(m.replaceAll("$1 убив $2"));
        } else {
            System.out.println("No match");
        }
    }
}

