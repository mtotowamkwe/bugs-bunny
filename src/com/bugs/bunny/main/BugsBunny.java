package com.bugs.bunny.main;

import com.bugs.bunny.controllers.ScreensController;
import com.bugs.bunny.controllers.SplashScreenController;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class BugsBunny extends Application {

    public static String splashScreenId = "splashScreen";
    public static String splashScreenFile = "../views/SplashScreen.fxml";
    public static String gitHubAuthScreenId = "gitHubAuthScreen";
    public static String gitHubAuthScreenFile = "../views/GitHubAuthentication.fxml";
    public static String bugsBunnyScreenId = "bugsBunnyScreen";
    public static String bugsBunnyScreenFile = "../views/BugsBunny.fxml";

    @Override
    public void start(Stage primaryStage) throws Exception {
        ScreensController mainScreensController = new ScreensController();
        mainScreensController.setHostServices(getHostServices());
        mainScreensController.loadScreen(splashScreenId, splashScreenFile);
        mainScreensController.setScreen(splashScreenId);

        Group root = new Group();
        root.getChildren().addAll(mainScreensController);

        Scene scene = new Scene(root);
        primaryStage.setTitle("Bugs Bunny");
        primaryStage.setScene(scene);
        primaryStage.show();

        if (SplashScreenController.getAccessTokenResult().get("isMissing")) {
            mainScreensController.loadScreen(gitHubAuthScreenId, gitHubAuthScreenFile);
            mainScreensController.setScreen(gitHubAuthScreenId);
        } else {
            mainScreensController.loadScreen(bugsBunnyScreenId, bugsBunnyScreenFile);
            mainScreensController.setScreen(bugsBunnyScreenId);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
