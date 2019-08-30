package com.bugs.bunny.controllers;

import com.bugs.bunny.DatabaseCalls.SQLiteDatabaseManager;
import com.bugs.bunny.environment.variables.OAuthCredentials;
import com.bugs.bunny.interfaces.ScreenTransitionManager;
import com.bugs.bunny.model.Issue;
import com.bugs.bunny.model.NewIssueErrorResponse;
import com.bugs.bunny.model.Repo;
import com.google.gson.Gson;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.Pair;
import org.controlsfx.control.Notifications;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class BugsBunnyController extends SQLiteDatabaseManager
        implements ScreenTransitionManager {
    private final String GITHUB_API_BASE_URL = "https://api.github.com/";

    @FXML Button newIssue;
    @FXML Button saveIssue;
    @FXML Button deleteIssue;
    @FXML TableView<Issue> table;
    @FXML TableColumn<Issue, String> title;
    @FXML TableColumn<Issue, String> date;
    @FXML TableColumn<Issue, String> state;
    @FXML TableColumn<Issue, String> label;
    @FXML TreeView<String> tree;
    @FXML Hyperlink project;
    @FXML Hyperlink issueId;
    @FXML TextField issueLabel;
    @FXML TextArea issueDescription;
    @FXML HBox titleLine;
    @FXML AnchorPane details;

    private ScreensController screensController;
    private Repo[] repos;
    private Issue[] issues;
    private String accessToken = OAuthCredentials.getAccessToken();
    private HostServices hostServices;
    private String selectedRepo;
    private NewIssueErrorResponse newIssueErrorResponse;
    private Issue currentSelectedIssue;

    public void initialize() {
        getRepos();

        saveIssue.setDisable(true);
        deleteIssue.setDisable(true);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Hide the details section on start-up
        details.setVisible(false);

        // Toggle visibility of the issue details section
        // Depending on whether an issue in the table above it
        // has been selected or not
        table.getSelectionModel().selectedItemProperty().addListener(
                (observableValue, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        saveIssue.setDisable(true);
                        deleteIssue.setDisable(false);
                        currentSelectedIssue = newSelection;

                        // Display details of the selected issue
                        details.setVisible(true);

                        // Populate the details section with details about the selected issue
                        issueId.setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent actionEvent) {
                                hostServices.showDocument(newSelection.getHtml_url());
                            }
                        });
                        issueId.setTooltip(getTooltip("issue"));

                        project.setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent actionEvent) {
                                hostServices.showDocument(newSelection.getHtml_url().replace("issues/1", ""));
                            }
                        });
                        project.setTooltip(getTooltip("project"));

                        issueDescription.setText(newSelection.getBody());

                        issueLabel.setText(newSelection.getAllIssueLabels());
                    } else {
                        // Set visibility of the details section to not visible
                        details.setVisible(false);
                    }
                }
        );
    }

    private Tooltip getTooltip(String name) {
        Tooltip tooltip = new Tooltip();
        StringBuilder tooltipBuilder = new StringBuilder();

        if (name.equals("issue")) {
            tooltipBuilder.append("Open the issue in your browser.");
        } else {
            tooltipBuilder.append("Open the repository in your browser.");
        }

        tooltip.setText(tooltipBuilder.toString());

        return tooltip;
    }

    @Override
    public void setScreenParent(ScreensController screenPage) {
        screensController = screenPage;
    }

    @Override
    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    private void getRepos() {
        StringBuilder sb = new StringBuilder();

        try {
            URL url = new URL("https://api.github.com/user/repos?access_token=" + accessToken);
            URLConnection conn = url.openConnection();
            InputStream is = conn.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            int value;
            while ((value = br.read()) > -1) {
                char c = (char) value;
                sb.append(c);
            }

            repos = new Gson().fromJson(sb.toString(), Repo[].class);
            populateListOfProjects(repos);
        } catch (MalformedURLException mue) {
            System.out.println("Wrong url (errror details):\n" + mue.getMessage());
            mue.printStackTrace();
        } catch (IOException ioe) {
            System.out.println("IOException occurred (error details):\n");
            ioe.printStackTrace();
        }
    }

    private void populateListOfProjects(Repo[] repos) {
        TreeItem<String> root = new TreeItem<>("Projects");
        root.setExpanded(true);

        for (Repo repo : repos) {
            TreeItem<String> item = new TreeItem<>(repo.getName());
            item.setExpanded(true);
            root.getChildren().add(item);
        }

        tree.setRoot(root);
        tree.setShowRoot(false);
        tree.getSelectionModel()
                .selectedItemProperty()
                .addListener((v, oldValue, newValue) -> {
                    if (newValue != null) {
                        saveIssue.setDisable(true);
                        getSelectedRepoIssues(newValue.getValue());
                        selectedRepo = newValue.getValue();
                    }
                });
    }

    private void getSelectedRepoIssues(String name) {
        StringBuilder issuesJson = new StringBuilder();

        for (Repo repo : repos) {
            if (repo.getName().equals(name)) {
                String issuesUrl = repo.getIssues_url().split("\\{")[0];

                try {
                    URL url = new URL(issuesUrl + "?state=all");
                    URLConnection urlConnection = url.openConnection();
                    InputStream inputStream = urlConnection.getInputStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                    int charCode;

                    while ((charCode = bufferedReader.read()) > -1) {
                        char chr = (char) charCode;
                        issuesJson.append(chr);
                    }

                    issues = new Gson().fromJson(
                            issuesJson.toString(),
                            Issue[].class
                    );

                    for (Issue i : issues) {
                        i.setAllIssueLabels(Issue.getAllLabelsInAnIssue(i.getLabels()));
                        i.setIssueCreatedDate(Issue.getDateIssueWasCreated(i.getCreated_at()));
                    }

                    populateTableOfIssues(issues);
                } catch (MalformedURLException mue) {
                    System.out.println("Check the url.");
                    mue.getMessage();
                    mue.printStackTrace();
                } catch (IOException ioe) {
                    System.out.println("IOException occurred.");
                    ioe.getMessage();
                    ioe.printStackTrace();
                }
            }
        }
    }

    private void populateTableOfIssues(Issue[] issues) {
        ObservableList<Issue> tableData = FXCollections.observableArrayList(issues);

        title.setCellValueFactory(new PropertyValueFactory<>("title"));

        date.setCellValueFactory(new PropertyValueFactory<>("issueCreatedDate"));

        state.setCellValueFactory(new PropertyValueFactory<>("state"));

        label.setCellValueFactory(new PropertyValueFactory<>("allIssueLabels"));

        table.setItems(tableData);

        // Adds a placeholder object whenever the table is empty
        // table.setPlaceholder();
    }

    public void createNewIssue(ActionEvent actionEvent) {
        if (selectedRepo == null) {
            notification(
                    "warning",
                    "New Issue Warning",
                    "Select a project from the " +
                            "provided list inorder to create " +
                            "an issue for it."
            );
            return;
        }

        AtomicBoolean hasNewIssueBeenCreated = new AtomicBoolean();
        Dialog<Pair<String, String>> newIssueDialog = new Dialog<>();

        newIssueDialog.setTitle("New Issue");
        newIssueDialog.setHeaderText(null);

        ButtonType okBtn = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);

        newIssueDialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(20, 150, 10, 10));

        TextField title = new TextField();
        title.setPromptText("Title.");
        TextField labels = new TextField();
        labels.setPromptText("For multiple labels use commas.");
        TextArea body = new TextArea();
        body.setPromptText("A description of the issue.");

        gridPane.add(new Label("Title"), 0, 0);
        gridPane.add(title, 1, 0);
        gridPane.add(new Label("Label(s)"), 0, 1);
        gridPane.add(labels, 1, 1);
        gridPane.add(new Label("Description"), 0, 2);

        // Test the row and column spans work as expected!!!
        gridPane.add(body, 0, 3, 2, 2);

        // Disable the OK button if the title or description is missing
        // otherwise enable it
        Node okButton = newIssueDialog.getDialogPane().lookupButton(okBtn);
        okButton.setDisable(true);

        title.textProperty().addListener((observable, oldValue, newValue) -> {
            okButton.setDisable(newValue.trim().isEmpty());
        });

        newIssueDialog.getDialogPane().setContent(gridPane);

        // Set the focus to the title textfield by default
        Platform.runLater(() -> title.requestFocus());

        // When the OK button is clicked get the title, labels and body
        // and pass them to a helper function that create a new issue using the GitHub API
        newIssueDialog.setResultConverter(clickedButton -> {
            if (clickedButton == okBtn) {
                // Create a brand new issue
                String[] newIssueLabels = getAllLabels(labels.getText());
                String newIssueTitle = title.getText();
                String newIssueBody = body.getText();

                String newIssueRepo = selectedRepo;
                String newIssueRepoOwner = "";

                for (Repo repo : repos) {
                    if (repo.getName().equals(newIssueRepo)) {
                        newIssueRepoOwner = repo.getRepoOwner();
                        break;
                    }
                }

                boolean isCreated = sendIssueRequest(
                        newIssueTitle, newIssueBody,
                        newIssueLabels, newIssueRepo,
                        newIssueRepoOwner, "create",
                        // Issue creation doesn't require an
                        // issue number hence default to zero
                        0
                );

                hasNewIssueBeenCreated.set(isCreated);
            }

            return null;
        });

        newIssueDialog.showAndWait();

        if (hasNewIssueBeenCreated.get()) {
            notification(
                    "information",
                    "New Issue Response",
                    "The issue was successfully added to the project " +
                            selectedRepo + "."
            );
            getSelectedRepoIssues(selectedRepo);
        } else {
            notification(
                    "error",
                    "New Issue Error",
                    newIssueErrorResponse.getMessage()
                    );
        }
    }

    private void notification(String notificationLevel, String title, String text) {
        Notifications notification = Notifications.create()
                .title(title)
                .darkStyle()
                .text(text)
                .position(Pos.CENTER);

        switch (notificationLevel) {
            case "warning":
                notification.showWarning();
                break;
            case "error":
                notification.showError();
                break;
            case "information":
                notification.showInformation();
                break;
            default:
                notification.show();
                break;
        }
    }

    private boolean sendIssueRequest(
            String title, String body, String[] labels,
            String repo, String owner, String action,
            int issueNumber) {
        StringBuilder responseJson = new StringBuilder();

        StringBuilder postBodyJson = new StringBuilder();
        postBodyJson.append("{\"title\":" + "\"" + title + "\"" + ",\"body\":" + "\"" + body + "\"" + ",\"labels\":[");
        for (String label : labels) {
            postBodyJson.append("\"" + label + "\"");
        }
        postBodyJson.append("]}");

        String postRequestBody = postBodyJson.
                toString().replaceAll(",]}$", "]}");

        byte[] postData = postRequestBody.getBytes(StandardCharsets.UTF_8);
        int postDataLength = postData.length;

        try {
            StringBuilder endpoint = new StringBuilder();
            endpoint.append("repos/");
            endpoint.append(owner);
            endpoint.append("/");
            endpoint.append(repo);
            endpoint.append("/issues");

            if (action.equals("update")) {
                endpoint.append("/");
                endpoint.append(issueNumber);
            }

            URL url = new URL(GITHUB_API_BASE_URL +
                            endpoint.toString());
            URLConnection urlConn = url.openConnection();
            HttpURLConnection httpUrlConn = (HttpURLConnection) urlConn;
            httpUrlConn.setRequestMethod("POST");
            httpUrlConn.setDoOutput(true);

            httpUrlConn.setFixedLengthStreamingMode(postDataLength);
            httpUrlConn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            httpUrlConn.setRequestProperty("Authorization", " token " + accessToken);
            httpUrlConn.connect();

            OutputStream os = httpUrlConn.getOutputStream();
            os.write(postData);

            int status = httpUrlConn.getResponseCode();

            if (String.valueOf(status).charAt(0) == '4' ||
                String.valueOf(status).charAt(0) == '5') {
                InputStream error = httpUrlConn.getErrorStream();
                InputStreamReader errorReader = new InputStreamReader(error);
                BufferedReader bufferedErrorReader = new BufferedReader(errorReader);

                int value;
                StringBuilder errorResponse = new StringBuilder();
                while ((value = bufferedErrorReader.read()) != -1) {
                    errorResponse.append((char) value);
                }

                newIssueErrorResponse = new Gson()
                        .fromJson(errorResponse.toString(), NewIssueErrorResponse.class);
                return false;
            }

        } catch (MalformedURLException mue) {
            System.out.println("There is problem with the url provided. See details below:\n" + mue.getMessage());
            mue.printStackTrace();
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
            ioe.printStackTrace();
        }

        return true;
    }

    private String[] getAllLabels(String text) {
        return text.split(",");
    }

    public void enableSaveButton(KeyEvent keyEvent) {
        saveIssue.setDisable(false);
    }

    public void saveIssue(ActionEvent actionEvent) {
        if (selectedRepo == null || currentSelectedIssue == null) {
            notification(
                    "warning",
                    "Save Issue Warning",
                    "You must first select an issue to be edited prior to saving."
            );
            return;
        }

        Alert confirmIssueUpdate = new Alert(Alert.AlertType.CONFIRMATION);
        confirmIssueUpdate.setTitle("Issue Update");
        confirmIssueUpdate.setHeaderText(null);
        confirmIssueUpdate.setContentText(
                "Are you sure you want to update the issue named: " + currentSelectedIssue.getTitle() + "?");

        Optional<ButtonType> selection = confirmIssueUpdate.showAndWait();

        if (selection.get() == ButtonType.OK) {
            // Prep owner and labels accordingly
            String[] labels = getAllLabels(issueLabel.getText());
            String owner = "";

            for (Repo repo : repos) {
                if (repo.getName().equals(selectedRepo)) {
                    owner = repo.getRepoOwner();
                    break;
                }
            }

            boolean isUpdated = sendIssueRequest(
                    currentSelectedIssue.getTitle(),
                    issueDescription.getText(),
                    labels,
                    selectedRepo,
                    owner,
                    "update",
                    currentSelectedIssue.getNumber()
            );

            if (isUpdated) {
                notification(
                        "information",
                        "Save Issue Response",
                        "The issue in the " +
                                "project " + selectedRepo +
                                " was successfully updated."
                );
                getSelectedRepoIssues(selectedRepo);
            } else {
                notification(
                        "error",
                        "Save Issue Error",
                        newIssueErrorResponse.getMessage()
                );
            }
        }
    }

    public void deleteIssue(ActionEvent actionEvent) {
        if (selectedRepo == null) {
            notification(
                    "warning",
                    "Delete Issue Warning",
                    "Select a project from the provided list inorder to delete an issue from it."
            );
            return;
        }

        Alert confirmIssueDeletion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmIssueDeletion.setTitle("Delete Issue");
        confirmIssueDeletion.setHeaderText(null);
        confirmIssueDeletion.setContentText(
                "Are you sure you want to delete the issue named: " +
                        currentSelectedIssue.getTitle() +
                        "?"
        );
        Optional<ButtonType> result = confirmIssueDeletion.showAndWait();

        if (result.get() == ButtonType.OK) {
            String endpoint = GITHUB_API_BASE_URL.concat("graphql");
            String issueId = currentSelectedIssue.getNode_id();
            String deleteIssueJsonString = "{" +
                "\"query\"" +
                ":" +
                "\"mutation{" +
                    "deleteIssue(input:{issueId:\\\"" + issueId + "\\\"}){" +
                        "repository{" +
                            "name" +
                        "}" +
                    "}" +
                "}\"" +
            "}";

            byte[] postPayload = deleteIssueJsonString
                    .getBytes(StandardCharsets.UTF_8);
            int length = postPayload.length;

            try {
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setFixedLengthStreamingMode(length);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Authorization", " bearer " + accessToken);
                conn.connect();

                OutputStream os = conn.getOutputStream();
                os.write(postPayload);

                int connStatus = conn.getResponseCode();

                if (String.valueOf(connStatus).charAt(0) == '4' ||
                    String.valueOf(connStatus).charAt(0) == '5') {
                    InputStream error = conn.getErrorStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(error);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                    int value;
                    StringBuilder response = new StringBuilder();

                    while ((value = bufferedReader.read()) != -1) {
                        response.append((char) value);
                    }

                    newIssueErrorResponse = new Gson()
                            .fromJson(response.toString(), NewIssueErrorResponse.class);

                    System.out.println("error message:\n" + newIssueErrorResponse.getMessage());
                } else {
                    notification(
                            "information",
                            "Delete Issue Response",
                            "The issue named " + currentSelectedIssue.getTitle() + " was successfully deleted."
                    );
                    getSelectedRepoIssues(selectedRepo);
                }
            } catch (MalformedURLException mue) {
                mue.printStackTrace();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
