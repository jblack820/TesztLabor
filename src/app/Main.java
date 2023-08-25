package app;


import controller.MainController;
import javafx.application.Application;
import javafx.stage.Stage;
import controller.LauncherPreloader;
import controller.PreloaderFXMLController;

/**
 *
 * @author takacs.gergely
 */
public class Main extends Application {     

    public static MainController controller = new MainController();
    
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
