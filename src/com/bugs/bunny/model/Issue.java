package com.bugs.bunny.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class Issue {
    private String node_id;
    private String url;
    private String repository_url;
    private String html_url;
    private int number;
    private String state;
    private String title;
    private String created_at;
    private String body;
    private transient String allIssueLabels;
    private transient String issueCreatedDate;
    private Label[] labels;

    public void setUrl(String url) {
        this.url = url;
    }

    public void setRepository_url(String repository_url) {
        this.repository_url = repository_url;
    }

    public void setHtml_url(String html_url) {
        this.html_url = html_url;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setLabels(Label[] labels) {
        this.labels = labels;
    }

    public String getUrl() {
        return url;
    }

    public String getRepository_url() {
        return repository_url;
    }

    public String getState() {
        return state;
    }

    public String getTitle() {
        return title;
    }

    public String getIssueCreatedDate() {
        return issueCreatedDate;
    }

    public String getHtml_url() {
        return html_url;
    }

    public String getCreated_at() {
        return created_at;
    }

    public String getBody() {
        return body;
    }

    public String getAllIssueLabels() {
        return allIssueLabels;
    }

    public void setAllIssueLabels(String allIssueLabels) {
        this.allIssueLabels = allIssueLabels;
    }

    public Label[] getLabels() {
        return labels;
    }

    public static String getAllLabelsInAnIssue(Label[] labels) {
        StringBuilder labelsString = new StringBuilder();

        for (Label l : labels) {
            labelsString.append(l.getName() + ", ");
        }

        String allLabels = labelsString.toString();
        return allLabels.substring(0, allLabels.length() - 2);
    }

    public void setIssueCreatedDate(String issueCreatedDate) {
        this.issueCreatedDate = issueCreatedDate;
    }

    public static String getDateIssueWasCreated(String date) {
        String prettyDate = "";
        try {
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
            String preferredDateString = getPreferredDateString(date);
            Date dateIssueWasCreated = format.parse(preferredDateString);
            prettyDate = format.format(dateIssueWasCreated);
        } catch (ParseException pex) {
            System.out.println("There was an issue parsing the date see details below:\n" +
                    pex.getMessage());
            pex.printStackTrace();
        }
        return prettyDate;
    }

    private static String getPreferredDateString(String date) {
        String newDate = date.substring(0, date.length() - 1).replaceAll("T", " ");
        LocalDateTime localDateTime = LocalDateTime.parse(newDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return localDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getNode_id() {
        return node_id;
    }

    public void setNode_id(String node_id) {
        this.node_id = node_id;
    }
}
