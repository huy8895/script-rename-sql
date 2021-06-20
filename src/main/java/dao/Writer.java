package dao;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class Writer {
    public static void write(List<String> stringList, String filePath) throws IOException {
        FileWriter writer = new FileWriter(filePath);
        BufferedWriter buffer = new BufferedWriter(writer);
        for (String line : stringList) {
            buffer.write(line);
            buffer.newLine();
        }
        buffer.close();
        System.out.println("Success...");
    }
}
