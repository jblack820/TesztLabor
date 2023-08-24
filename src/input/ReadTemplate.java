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
        System.out.println("File exists: " + file.exists());
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Thread: " + Thread.currentThread().getName());
        boolean b =  inputStream!=null;
        System.out.println("Fileinputstream ready:" + b);
        try {
            document = new XWPFDocument(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("beolvasta");

        return document;
    }

}

