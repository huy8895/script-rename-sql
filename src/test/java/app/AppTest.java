package app;

import junit.framework.TestCase;
import org.junit.Test;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class AppTest extends TestCase {

    @Test
    public void test1(){
        String districName = "Quận Long Biên";
        final String[] split = districName.replaceAll("\"", "")
                                          .split("\\s+");
        for (int i = 0; i < split.length; i++){
            System.out.println("split = " + split[i].toUpperCase().charAt(0));
        }
    }
    @Test
    public void test2(){

        System.out.println(deAccent("THANT000PĐ"));
    }

    public static String deAccent(String str) {
        String temp = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").toLowerCase().replaceAll(" ", "-").replaceAll("đ", "d");
    }


}