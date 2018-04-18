import history.StatisticsManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import timeline.Timeline;

import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("./sample.fxml"));
        primaryStage.setTitle("Hello World");
        primaryStage.setScene(new Scene(root, 300, 275));
        primaryStage.show();
    }

    public static void main(String[] args) {
        TrackingService trackingService = new TrackingService("chrome", "idea64");
        trackingService.start();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        trackingService.stop();
        Timeline chromeStatistics = trackingService.getStatisticsForApp("chrome");
        Timeline ideaStatistics = trackingService.getStatisticsForApp("idea64");
        System.out.println("ComputerStatistics: " + trackingService.getComputerStatistics().toString());
        System.out.println("Chrome background time: " + chromeStatistics.getRunningTimeInSec());
        System.out.println("Chrome foreground time: " + chromeStatistics.getActiveTimeInSec());
        System.out.println("IDEA background time: " + ideaStatistics.getRunningTimeInSec());
        System.out.println("IDEA foreground time: " + ideaStatistics.getActiveTimeInSec());

        try {
            StatisticsManager.save("chrome", chromeStatistics);
        } catch (IOException e) {
            e.printStackTrace();
        }

        launch(args);
    }
}
