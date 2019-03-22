package com.mabl.domain;

import java.util.Set;

public class CreateDeploymentPayload {
    public final String environmentId;
    public final String applicationId;
    public final Set<String> planLabels;
    public final CreateDeploymentProperties properties;

    public CreateDeploymentPayload(
            String environmentId,
            String applicationId,
            Set<String> planLabels,
            CreateDeploymentProperties properties
    ) {
        this.environmentId = environmentId;
        this.applicationId = applicationId;
        this.planLabels = planLabels;
        this.properties = properties;
    }
}