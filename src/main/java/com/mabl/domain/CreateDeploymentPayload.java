package com.mabl.domain;

import java.util.List;

public class CreateDeploymentPayload {
    public final String uri;
    public final String environmentName;
    public final String applicationName;
    public final CreateDeploymentProperties properties;
    public final List<List<String>> planTags;

    public CreateDeploymentPayload(
            String uri,
            String environmentName,
            String applicationName,
            CreateDeploymentProperties properties,
            List<List<String>> planTags
    ) {
        this.uri = uri;
        this.environmentName = environmentName;
        this.applicationName = applicationName;
        this.properties = properties;
        this.planTags = planTags;
    }
}