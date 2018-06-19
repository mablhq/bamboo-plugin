package com.mabl.domain;

import java.lang.reflect.Field;

public class CreateDeploymentProperties {
    private String deploymentOrigin;
    private String repositoryBranchName;
    private String repositoryRevisionNumber;
    private String repositoryUrl;
    private String repositoryName;
    private String repositoryPreviousRevisionNumber;
    private String repositoryCommitUsername;
    private String buildPlanId;
    private String buildPlanName;
    private String buildPlanJobId;
    private String buildPlanJobName;
    private String buildPlanNumber;
    private String buildPlanResultUrl;
    private CreateDeploymentProperties properties;

    public void setDeploymentOrigin(String plugin) {
        this.deploymentOrigin = plugin;
    }

    public void setRepositoryBranchName(String repositoryBranchName) {
        this.repositoryBranchName = repositoryBranchName;
    }

    public void setRepositoryRevisionNumber(String repositoryRevisionNumber) {
        this.repositoryRevisionNumber = repositoryRevisionNumber;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public void setRepositoryPreviousRevisionNumber(String repositoryPreviousRevisionNumber) {
        this.repositoryPreviousRevisionNumber = repositoryPreviousRevisionNumber;
    }

    public void setRepositoryCommitUsername(String repositoryCommitUsername) {
        this.repositoryCommitUsername = repositoryCommitUsername;
    }

    public void setBuildPlanId(String buildPlanId) {
        this.buildPlanId = buildPlanId;
    }

    public void setBuildPlanName(String buildPlanName) {
        this.buildPlanName = buildPlanName;
    }

    public void setBuildPlanJobId(String buildPlanJobId) {
        this.buildPlanJobId = buildPlanJobId;
    }

    public void setBuildPlanJobName(String buildPlanJobName) {
        this.buildPlanJobName = buildPlanJobName;
    }

    public void setBuildPlanNumber(String buildPlanNumber) {
        this.buildPlanNumber = buildPlanNumber;
    }

    public void setBuildPlanResultUrl(String buildPlanResultUrl) {
        this.buildPlanResultUrl = buildPlanResultUrl;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for(Field field : this.getClass().getDeclaredFields()) {
            try {
                if(field.get(this) != null) {
                    stringBuilder.append(field.getName());
                    stringBuilder.append("=");
                    stringBuilder.append(field.get(this).toString());
                    stringBuilder.append(", ");
                }
            } catch (IllegalAccessException e) {
            }
        }

        return stringBuilder.toString();
    }
}