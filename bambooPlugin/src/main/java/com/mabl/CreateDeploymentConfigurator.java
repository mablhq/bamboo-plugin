package com.mabl;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.endsWith;

public class CreateDeploymentConfigurator extends AbstractTaskConfigurator {

    public Map<String, String> generateTaskConfigMap(
            @NotNull final ActionParametersMap params,
            @Nullable final TaskDefinition previousTaskDefinition
    ) {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
        config.put("restApiKey", params.getString("restApiKey"));
        config.put("environmentId", params.getString("environmentId"));
        config.put("applicationId", params.getString("applicationId"));
        return config;
    }

    @Override
    public void populateContextForCreate(@NotNull final Map<String, Object> context) {
        super.populateContextForCreate(context);

        context.put("restApiKey", "");
        context.put("environmentId", "");
        context.put("applicationId", "");
    }

    @Override
    public void populateContextForEdit(
            @NotNull final Map<String, Object> context,
            @NotNull final TaskDefinition taskDefinition
    ) {
        super.populateContextForEdit(context, taskDefinition);
        context.put("restApiKey", taskDefinition.getConfiguration().get("restApiKey"));
        context.put("environmentId", taskDefinition.getConfiguration().get("environmentId"));
        context.put("applicationId", taskDefinition.getConfiguration().get("applicationId"));
    }

    @Override
    public void validate(
            @NotNull final ActionParametersMap params,
            @NotNull final ErrorCollection errorCollection
    ) {
        super.validate(params, errorCollection);

        final String restApiKeyValue = params.getString("restApiKey");
        if(isEmpty(restApiKeyValue)) {
            errorCollection.addError("restApiKey", "RestApiKey is required.");
        }
        if(!restApiKeyIsValid(restApiKeyValue)) {
            errorCollection.addError("restApiKey", "The entered RestApiKey is invalid.");
        }

        final String environmentIdValue = params.getString("environmentId");
        if(!environmentIdIsValid(environmentIdValue)) {
            errorCollection.addError("environmentId", "The entered EnvironmentId is invalid.");

        }
        final String applicationIdValue = params.getString("applicationId");
        if(!applicationIdIsValid(applicationIdValue)) {
            errorCollection.addError("applicationId", "The entered ApplicationId is invalid.");
        }

        if(isEmpty(environmentIdValue) && isEmpty(applicationIdValue)) {
            String error = "One of ApplicationId or EnvironmentId is required.";
            errorCollection.addError("environmentId", error);
            errorCollection.addError("applicationId", error);
        }
    }

    private boolean restApiKeyIsValid(String restApiKey) {
        try(RestApiClient apiClient = new RestApiClient(restApiKey)) {
            String organizationId = apiClient.getApiKeyResult(restApiKey).organization_id;
            return !isEmpty(organizationId ) && endsWith(organizationId, "-w");
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean environmentIdIsValid(String environmentId) {
        return isEmpty(environmentId) || endsWith(environmentId, "-e");
    }

    private boolean applicationIdIsValid(String applicationId) {
        return isEmpty(applicationId) || endsWith(applicationId, "-a");
    }
}
