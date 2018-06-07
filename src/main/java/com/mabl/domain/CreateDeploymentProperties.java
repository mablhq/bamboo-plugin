package com.mabl.domain;

public class CreateDeploymentProperties {
    private String deploymentSource;
    private String branchName;
    private String revisionNumber;
    private String repositoryUrl;
    private String repositoryName;
    private String previousRevisionNumber;
    private String commitUsername;
    private String buildPlanNumber;
    private String buildPlanKey;
    private String buildResultUrl;


    public void setDeploymentSource(String pluginSource) {
        this.deploymentSource = pluginSource;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public void setRevisionNumber(String revisionNumber) {
        this.revisionNumber = revisionNumber;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public void setPreviousRevisionNumber(String previousRevisionNumber) {
        this.previousRevisionNumber = previousRevisionNumber;
    }

    public void setCommitUsername(String commitUsername) {
        this.commitUsername = commitUsername;
    }

    public void setBuildPlanNumber(String buildPlanNumber) {
        this.buildPlanNumber = buildPlanNumber;
    }

    public void setBuildPlanKey(String buildPlanKey) {
        this.buildPlanKey = buildPlanKey;
    }

    public void setBuildResultUrl(String buildResultUrl) {
        this.buildResultUrl = buildResultUrl;
    }
}
