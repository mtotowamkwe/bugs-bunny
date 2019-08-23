package com.bugs.bunny.model;

public class Repo {
    private String name;
    private String html_url;
    private String issues_url;
    private boolean has_issues;
    private int open_issues_count;
    private RepoOwner owner;

    public String getName() {
        return name;
    }

    public String getIssues_url() {
        return issues_url;
    }

    public String getRepoOwner() {
        return this.owner.getLogin();
    }
}
