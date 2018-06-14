package com.mabl.domain;

import java.util.List;

public class CreateDeploymentPayload {
    public final String environmentId;
    public final String applicationId;
    public final CreateDeploymentProperties properties;
    public final List<List<String>> planTags;

    public CreateDeploymentPayload(
            String environmentId,
            String applicationId,
            CreateDeploymentProperties properties,
            List<List<String>> planTags
    ) {
        this.environmentId = environmentId;
        this.applicationId = applicationId;
        this.properties = properties;
        this.planTags = planTags;
    }
}