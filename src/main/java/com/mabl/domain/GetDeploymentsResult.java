package com.mabl.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;
import java.util.List;

public class GetDeploymentsResult implements ApiResult {
    public List<Deployment> deployments;

    @JsonCreator
    public GetDeploymentsResult(
            @JsonProperty("environments") final List<Deployment> deployments
    ) {
        this.deployments = deployments;
    }

    @SuppressWarnings("WeakerAccess")
    public static class Deployment {
        public final String id;
        public final String name;
        public final Long createdTime;
        public final String createdById;
        public final Long lastUpdatedTime;
        public final String lastUpdatedById;
        public final String organizationId;
        public final String applicationId;
        public final String environmentId;
        public final Collection<String> tags;
        public final String uri;
        public final Collection<Object> variables;

        @JsonCreator
        public Deployment(
                @JsonProperty("id") final String id,
                @JsonProperty("name") final String name,
                @JsonProperty("created_time") final Long created_time,
                @JsonProperty("created_by_id") final String created_by_id,
                @JsonProperty("last_updated_time") final Long last_updated_time,
                @JsonProperty("last_updated_by_id") final String last_updated_by_id,
                @JsonProperty("organization_id") final String organization_id,
                @JsonProperty("application_id") final String application_id,
                @JsonProperty("environment_id") final String environment_id,
                @JsonProperty("tags") final Collection<String> tags,
                @JsonProperty("uri") final String uri,
                @JsonProperty("variables") final Collection<Object> variables
        ) {
            this.id = id;
            this.name = name;
            this.createdTime = created_time;
            this.createdById = created_by_id;
            this.lastUpdatedTime = last_updated_time;
            this.lastUpdatedById = last_updated_by_id;
            this.organizationId = organization_id;
            this.applicationId = application_id;
            this.environmentId = environment_id;
            this.tags = tags;
            this.uri = uri;
            this.variables = variables;
        }
    }

}
