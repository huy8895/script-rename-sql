package app;

import junit.framework.TestCase;
import org.junit.Test;

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
}