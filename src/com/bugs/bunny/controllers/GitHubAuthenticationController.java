package com.bugs.bunny.controllers;

import com.bugs.bunny.DatabaseCalls.SQLiteDatabaseManager;
import com.bugs.bunny.environment.variables.OAuthCredentials;
import com.bugs.bunny.interfaces.ScreenTransitionManager;
import com.bugs.bunny.main.BugsBunny;
import javafx.application.HostServices;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class GitHubAuthenticationController extends SQLiteDatabaseManager
        implements ScreenTransitionManager {
    @FXML WebView gitHubAuthWindow;
    private ScreensController screensController;
    private String gitHubOAuthCode;
    private String gitHubAccessToken;
    private ArrayList<String> grantedScopes = new ArrayList<>();
    private OAuthCredentials oAuthCredentials = new OAuthCredentials();

    public void initialize() {
        openGitHubOAuthPage();
    }

    @Override
    public void setScreenParent(ScreensController screenPage) {
        screensController = screenPage;
    }

    @Override
    public void setHostServices(HostServices hostServices) {
    }

    private String getClientId() {
        return oAuthCredentials.getClientId();
    }

    private String getClientSecret() {
        return oAuthCredentials.getClientSecret();
    }

    public void openGitHubOAuthPage() {
        String clientId = getClientId();
        String clientSecret = getClientSecret();
        String url = "http://localhost:8080/bugs_bunny_war_exploded/";
        gitHubAuthWindow.getEngine().load(url);

        gitHubAuthWindow.getEngine().getLoadWorker().stateProperty().addListener(
                (ov, oldState, newState) -> {
                    if (newState == Worker.State.SUCCEEDED) {
                        String url1 = gitHubAuthWindow.getEngine().getLocation();
                        if (url1.contains("callback")) {
                            gitHubOAuthCode = getGitHubOAuthCode(url1);
                            String gitHubApiResponse = getGitHubApiResponse(clientId, clientSecret, gitHubOAuthCode);
                            getGrantedScopes(gitHubApiResponse);
                            screensController.loadScreen(BugsBunny.bugsBunnyScreenId, BugsBunny.bugsBunnyScreenFile);
                            screensController.setScreen(BugsBunny.bugsBunnyScreenId);
                        }
                    }
                }
        );
    }

    private String getGitHubOAuthCode(String callbackUrl) {
        return callbackUrl.split("=")[1];
    }

    private String getGitHubApiResponse(String id, String secret, String code) {
        StringBuilder accessToken = new StringBuilder();
        String postRequestParameters = "client_id=" + id + "&client_secret=" + secret + "&code=" + code;
        byte[] postData = postRequestParameters.getBytes(StandardCharsets.UTF_8);
        int postDataLength = postData.length;
        String requestURL = "https://github.com/login/oauth/access_token";

        try {
            URL url = new URL(requestURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("charset", "utf-8");
            conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
            conn.setUseCaches(false);
            conn.getOutputStream().write(postData);
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

            int value;
            while ((value = in.read()) != -1) {
                accessToken.append((char) value);
            }
        } catch (MalformedURLException muex) {
            System.out.println("There is an issue with the URL check error details below.\n" + muex.getMessage());
            muex.printStackTrace();
        } catch (IOException ioe) {
            System.out.println("An I/O issue occurred see error details below.\n");
            ioe.printStackTrace();
        }

        return accessToken.toString();
    }

    private void getGrantedScopes(String accessToken) {
        String[] responseFromGitHubApi = accessToken.split("&");

        for (String element : responseFromGitHubApi) {
            String[] elementArray = element.split("=");

            if (elementArray[0].equals("access_token")) {
                gitHubAccessToken = elementArray[1];
                OAuthCredentials.setAccessToken(gitHubAccessToken);
            } else if (elementArray[0].equals("scope")){
                String[] elementArrayScopes = elementArray[1].split("%2C");

                for (String scope : elementArrayScopes) {
                    if (scope.contains("user")) {
                        grantedScopes.add("user");
                    } else if (scope.contains("repo")) {
                        grantedScopes.add("repo");
                    } else if (scope.contains("notifications")) {
                        grantedScopes.add("notifications");
                    } else if (scope.contains("gist")) {
                        grantedScopes.add("gist");
                    } else if (scope.contains("delete_repo")) {
                        grantedScopes.add("delete_repo");
                    } else if (scope.contains("admin") && scope.contains("org")) {
                        grantedScopes.add("admin:org");
                    } else {
                        System.out.println("No scopes were granted by the user.");
                    }
                }
            }
        }
    }
}
