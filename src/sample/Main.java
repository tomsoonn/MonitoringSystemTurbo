package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Date;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("Hello World");
        primaryStage.setScene(new Scene(root, 300, 275));
        primaryStage.show();
    }


    public static void main(String[] args) {

//        Date systemStartTime = new Date();
//        ComputerStatistics computerStatistics = new ComputerStatistics(systemStartTime);
//        Runtime.getRuntime().addShutdownHook(new ShutDownThread(computerStatistics));
//        launch(args);

        ApplicationStatistics applicationStatistics = new ApplicationStatistics("idea64.exe");
        System.out.println(applicationStatistics.isRunning());
        System.out.println(applicationStatistics.isActive());

    }
}
