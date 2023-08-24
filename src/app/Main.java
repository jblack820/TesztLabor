package app;


import com.sun.javafx.application.LauncherImpl;
import controller.TestCenterController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import testcenterfx.LauncherPreloader;
import testcenterfx.PreloaderFXMLController;

/**
 *
 * @author takacs.gergely
 */
public class Main extends Application {     

    public static TestCenterController controller = new TestCenterController();
    
    public static Stage primaryStage = null;

    @Override
    public void init() throws Exception {        
        PreloaderFXMLController preloaderController = new PreloaderFXMLController();
        preloaderController.start();
    }
    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
    }
    public static void main(String[] args) {
        System.setProperty("javafx.preloader", LauncherPreloader.class.getCanonicalName());
        Application.launch(Main.class, args);
    }
}
