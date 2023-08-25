package input;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

public class ReadTemplate {

    public XWPFDocument getTemplate(String resourceName) {
        XWPFDocument document = null;

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(resourceName).getFile());
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        try {
            document = new XWPFDocument(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return document;
    }

}

