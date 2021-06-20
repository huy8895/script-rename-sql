package dao;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Reader {
    public static List<String> read(String filePath) throws IOException {
        FileReader fr = new FileReader(filePath);
        BufferedReader br = new BufferedReader(fr);
        String line;
        ArrayList<String> listLine = new ArrayList<>();
        while ((line = br.readLine()) != null){
            listLine.add(line);
        }
        br.close();
        fr.close();
        return listLine.stream()
                       .distinct()
                       .collect(Collectors.toList());
    }

}
