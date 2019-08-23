package com.bugs.bunny.controllers;

import com.bugs.bunny.interfaces.ScreenTransitionManager;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.HostServices;
import javafx.beans.property.DoubleProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;
import java.util.HashMap;

public class ScreensController extends StackPane {
    private HashMap<String, Node> screens = new HashMap<>();
    private HostServices hostServices;


    public ScreensController() {
        super();
    }

    public void addScreen(String name, Node screen) {
        screens.put(name, screen);
    }

    public Node getScreen(String name) {
        return screens.get(name);
    }

    public boolean loadScreen(String name, String resource) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(resource));
            Parent loadScreen = fxmlLoader.load();
            ScreenTransitionManager screenTransitionManager = ((ScreenTransitionManager) fxmlLoader.getController());
            screenTransitionManager.setHostServices(hostServices);
            screenTransitionManager.setScreenParent(this);
            addScreen(name, loadScreen);
            return true;
        } catch (IOException ioe) {
            System.out.println(ioe.getLocalizedMessage());
            System.out.println(ioe.getMessage());
            ioe.printStackTrace();
            return false;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean setScreen(final String name) {
        if (screens.containsKey(name)) {
            final DoubleProperty opacity = opacityProperty();

            if (!getChildren().isEmpty()) {
                Timeline fade = new Timeline(
                        new KeyFrame(Duration.ZERO, new KeyValue(opacity, 1.0)),
                        new KeyFrame(new Duration(1000), new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent actionEvent) {
                                getChildren().remove(0);
                                getChildren().add(0, screens.get(name));
                                Timeline fadeIn = new Timeline(
                                        new KeyFrame(Duration.ZERO, new KeyValue(opacity, 0.0)),
                                        new KeyFrame(new Duration(800), new KeyValue(opacity, 1.0))
                                );
                                fadeIn.play();
                            }
                        }, new KeyValue(opacity, 0.0))
                );
                fade.play();
            } else {
                setOpacity(0.0);
                getChildren().add(screens.get(name));
                Timeline fadeIn = new Timeline(
                        new KeyFrame(Duration.ZERO, new KeyValue(opacity, 0.0)),
                        new KeyFrame(new Duration(2500), new KeyValue(opacity, 1.0))
                );
                fadeIn.play();
            }
            return true;
        } else {
            System.out.println("The screen " + name + " has not been loaded. Ensure it's fxml file exists.");
            return false;
        }
    }

    public boolean unloadScreen(String name) {
        if (screens.containsKey(name)) {
            screens.remove(name);
            return true;
        }

        System.out.println("The screen " + name + " does not exist thus could not be removed.");
        return false;
    }

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }
}
