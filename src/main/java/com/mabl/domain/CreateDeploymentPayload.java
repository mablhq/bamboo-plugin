package com.mabl.domain;

import java.util.HashMap;

public class CreateDeploymentPayload {
    public final String environmentId;
    public final String applicationId;
    public final HashMap<String, String> properties;

    public CreateDeploymentPayload(
            String environmentId,
            String applicationId,
            HashMap<String, String> properties
    ) {
        this.environmentId = environmentId;
        this.applicationId = applicationId;
        this.properties = properties;
    }
}