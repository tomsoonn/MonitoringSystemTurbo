package monitoringsystemturbo.presenter;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import monitoringsystemturbo.config.ConfigManager;
import monitoringsystemturbo.controller.AddApplicationController;
import monitoringsystemturbo.controller.MainController;
import monitoringsystemturbo.exporter.MainExporter;
import monitoringsystemturbo.history.StatisticsManager;
import monitoringsystemturbo.model.TrackingService;
import monitoringsystemturbo.model.app.Application;
import monitoringsystemturbo.model.computer.ComputerStatistics;
import monitoringsystemturbo.model.timeline.Timeline;
import monitoringsystemturbo.presenter.timeline.PeriodColor;
import monitoringsystemturbo.presenter.timeline.TimelineElement;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static monitoringsystemturbo.utils.IconConverter.iconToFxImage;

public class MainPresenter {

    @FXML
    private Legend timelineLegend;
    @FXML
    private Pane computerTimelineContainer;
    @FXML
    private ScrollPane appTimelineContainer;
    @FXML
    private VBox appTimelineList;
    @FXML
    private DatePicker datePicker;
    private Integer currentDay;


    private TrackingService trackingService;
    private MainExporter mainExporter;
    private List<Application> loadedApplications;
    private AddApplicationController addApplicationController;
    private List<TimelineElement> timelineElements;

    @FXML
    private ListView<Application> applicationList;
    private MainController mainController;

    @FXML
    public void initialize(TrackingService trackingService, MainExporter mainExporter, List<Application> loadedApplications) {
        this.trackingService = trackingService;
        this.mainExporter = mainExporter;
        this.loadedApplications = loadedApplications;
        setCellFactory();
        applicationList.setItems(FXCollections.observableList(loadedApplications));
        renderTimelineLegend();
        initializeTimelines();
        initializeDatePicker();

    }

    private void initializeAppsToMonitor() {
        for (Application application : loadedApplications) {
            trackingService.addAppToMonitor(application.getName());
        }
    }

    private void renderTimelineLegend() {
        timelineLegend.addElement(PeriodColor.runningColor, "Running");
        timelineLegend.addElement(PeriodColor.activeColor, "Active");
    }

    private void initializeTimelines() {
        timelineElements = new ArrayList<>();
        appTimelineContainer.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        try {
            List<ComputerStatistics> computerStatistics = StatisticsManager.loadComputerStats();
            Timeline timeline = new Timeline(computerStatistics);
            TimelineElement timelineElement = new TimelineElement("Computer", Arrays.asList(timeline));
            timelineElement.setTimelineViewWidthByRegion(computerTimelineContainer);
            computerTimelineContainer.getChildren().add(timelineElement);
            timelineElements.add(timelineElement);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            for (Application application : loadedApplications) {
                application.createFileIfNeeded();
                List<Timeline> timelines = StatisticsManager.load(application.getName());
                TimelineElement timelineElement = new TimelineElement(application.getName(), timelines);
                timelineElement.setTimelineViewWidthByRegion(appTimelineContainer);
                appTimelineList.getChildren().add(timelineElement);
                timelineElements.add(timelineElement);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeDatePicker() {
        datePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            Integer day = (int) newValue.toEpochDay();
            timelineElements.forEach(timelineElement -> timelineElement.showDay(day));
            currentDay = day;
        });
        datePicker.setValue(LocalDate.now()); // 24.04.2018
    }

    @FXML
    public void onPrevDay() {
        datePicker.setValue(LocalDate.ofEpochDay(currentDay - 1));
    }

    @FXML
    public void onNextDay() {
        if (!LocalDate.ofEpochDay(currentDay).equals(LocalDate.now()))
            datePicker.setValue(LocalDate.ofEpochDay(currentDay + 1));
    }

    @FXML
    public void onAddApplication() throws IOException, ClassNotFoundException {

        addApplicationController.showAddView();
        Application application = addApplicationController.getNewApplication();
        if (application != null) {
            application.createFileIfNeeded();
            loadedApplications.add(application);

            applicationList.setItems(FXCollections.observableList(loadedApplications));

            ConfigManager.save(loadedApplications);

            List<Timeline> timelines = StatisticsManager.load(application.getName());
            TimelineElement timelineElement = new TimelineElement(application.getName(), timelines);
            timelineElement.setTimelineViewWidthByRegion(appTimelineContainer);
            appTimelineList.getChildren().add(timelineElement);
            timelineElements.add(timelineElement);

            trackingService.addAppToMonitor(application.getName());
        }
    }

    @FXML
    public void onRemoveApplication() throws IllegalStateException {
        Application application = applicationList.getSelectionModel().getSelectedItem();
        try {
            StatisticsManager.save(application.getName(), trackingService.getStatisticsForApp(application.getName()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        trackingService.stopAppMonitoring(application.getName());

        loadedApplications.remove(application);

        applicationList.setItems(FXCollections.observableList(loadedApplications));

        //TODO remove from config.json

        TimelineElement timelineElement = timelineElements.stream()
                .filter(element -> element.getName().equals(application.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cannot delete app which are not in list"));

        timelineElements.remove(timelineElement);
        appTimelineList.getChildren().remove(timelineElement);
    }

    @FXML
    public void onExport() {
        ExportWindowHandler exportWindowHandler = new ExportWindowHandler();
        List<String> applicationsToExport = exportWindowHandler.displayCheckingWindow(trackingService.getApplicationsNames());
        if (!exportWindowHandler.getCancelValue()) {
            try {
                mainExporter.export(trackingService, applicationsToExport);
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Success!");
                successAlert.setHeaderText(null);
                successAlert.setContentText("Data exported successfully! ");
                successAlert.showAndWait();
            } catch (IOException e) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error!");
                errorAlert.setHeaderText(null);
                errorAlert.setContentText("Error occurred while exporting data.");
                errorAlert.showAndWait();
            }
        }
    }

    private void setCellFactory() {
        applicationList.setCellFactory(param -> new ListCell<Application>() {
            @Override
            public void updateItem(Application application, boolean empty) {
                super.updateItem(application, empty);

                ImageView imageView = new ImageView();
                Label label = new Label();
                VBox vbox = new VBox();

                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Image image = iconToFxImage(application.findIcon());
                    label.setText(application.getName());
                    imageView.setImage(image);
                    vbox.setAlignment(Pos.CENTER);
                    vbox.getChildren().add(imageView);
                    vbox.getChildren().add(label);
                    setGraphic(vbox);
                }
            }
        });
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setAddApplicationController(AddApplicationController addApplicationController) {
        this.addApplicationController = addApplicationController;
    }
}
