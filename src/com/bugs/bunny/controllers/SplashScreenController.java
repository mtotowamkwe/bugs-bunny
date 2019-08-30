package com.bugs.bunny.controllers;

import com.bugs.bunny.DatabaseCalls.SQLiteDatabaseManager;
import com.bugs.bunny.environment.variables.OAuthCredentials;
import com.bugs.bunny.interfaces.ScreenTransitionManager;
import com.bugs.bunny.main.BugsBunny;
import com.bugs.bunny.model.Databases.SQLite.OAuthCredentialsDatabaseSchema.OAuthCredentialsTable;
import com.bugs.bunny.model.Databases.SQLite.OAuthCredentialsDatabaseSchema.OAuthCredentialsTableColumns;
import javafx.application.HostServices;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

public class SplashScreenController extends SQLiteDatabaseManager implements ScreenTransitionManager {
    private static ScreensController screensController;
    private Connection connection = super.sqliteDatabaseConnection;
    OAuthCredentials oAuthCredentials = new OAuthCredentials();
    public static HashMap<String, Boolean> accessTokenResult = new HashMap<>();

    public static HashMap<String, Boolean> getAccessTokenResult() {
        return accessTokenResult;
    }

    @Override
    public void setScreenParent(ScreensController screenPage) {
        screensController = screenPage;
        exitSplashScreen();
    }

    @Override
    public void setHostServices(HostServices hostServices) {

    }

    private void exitSplashScreen() {
        if (connection != null) {
            super.createSqliteDatabaseTable(connection);
            oAuthCredentials.updateSqliteDatabaseTable(
                    connection,
                    new String[] {
                            OAuthCredentialsTableColumns.CLIENT_ID,
                            OAuthCredentialsTableColumns.CLIENT_SECRET
                    }
            );
            if (!accessTokenExists(connection)) {
                accessTokenResult.put("isMissing", true);
            } else {
                accessTokenResult.put("isMissing", false);
            }

            try {
                connection.close();
            } catch (SQLException sqlex) {
                sqlex.printStackTrace();
            }
        }
    }

    private boolean accessTokenExists(Connection connection) {
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("select * from " + OAuthCredentialsTable.NAME + ";");

            while (resultSet.next()) {
                if (resultSet.getString("encrypted_access_token") != null) {
                    return true;
                }
            }
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
        }

        return false;
    }
}