package com.mabl.domain;

public class CreateDeploymentPayload {
    public final String environmentId;
    public final String applicationId;
    public final CreateDeploymentProperties properties;

    public CreateDeploymentPayload(
            String environmentId,
            String applicationId,
            CreateDeploymentProperties properties
    ) {
        this.environmentId = environmentId;
        this.applicationId = applicationId;
        this.properties = properties;
    }
}