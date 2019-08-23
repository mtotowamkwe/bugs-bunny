package com.bugs.bunny.main;

import com.bugs.bunny.controllers.ScreensController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class BugsBunny extends Application {

    public static String gitHubAuthScreenId = "gitHubAuthScreen";
    public static String gitHubAuthScreenFile = "../views/GitHubAuthentication.fxml";
    public static String bugsBunnyScreenId = "bugsBunnyScreen";
    public static String bugsBunnyScreenFile = "../views/BugsBunny.fxml";

    @Override
    public void start(Stage primaryStage) throws Exception {
        ScreensController mainScreensController = new ScreensController();
        mainScreensController.setHostServices(getHostServices());
        mainScreensController.loadScreen(BugsBunny.gitHubAuthScreenId, BugsBunny.gitHubAuthScreenFile);
        mainScreensController.setScreen(BugsBunny.gitHubAuthScreenId);

        Group root = new Group();
        root.getChildren().addAll(mainScreensController);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
