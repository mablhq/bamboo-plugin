package com.mabl.domain;

public class CreateDeploymentPayload {
    public final String environmentId;
    public final String applicationId;

    public CreateDeploymentPayload(
            String environmentId,
            String applicationId
    ) {
        this.environmentId = environmentId;
        this.applicationId = applicationId;
    }
}