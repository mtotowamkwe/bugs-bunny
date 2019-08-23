package com.bugs.bunny.interfaces;

import com.bugs.bunny.controllers.ScreensController;
import javafx.application.HostServices;

public interface ScreenTransitionManager {
    void setScreenParent(ScreensController screenPage);
    void setHostServices(HostServices hostServices);
}
