package com.mabl;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

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
    public void validate(
            @NotNull final ActionParametersMap params,
            @NotNull final ErrorCollection errorCollection
    ) {
        super.validate(params, errorCollection);

        final String restApiKeyValue = params.getString("restApiKey");
        if(StringUtils.isEmpty(restApiKeyValue)) {
            errorCollection.addError("restApiKey", "DEBUG fill in restApiKeyError Message");
        }

        final String environmentIdValue = params.getString("environmentId");
        final String applicationIdValue = params.getString("applicationId");
        if(StringUtils.isEmpty(environmentIdValue) && StringUtils.isEmpty(applicationIdValue)) {
            errorCollection.addError("environmentId", "DEBUG fill in environmentId Message");
            errorCollection.addError("applicationId", "DEBUG fill in applicationId Message");
        }
    }

    @Override
    public void populateContextForCreate(@NotNull final Map<String, Object> context) {
        super.populateContextForCreate(context);

        context.put("restApiKey", "ApiKey");
        context.put("environmentId", "Input an environmentId (ends with -e)");
        context.put("applicationId", "Input an applicationId (ends with -a)");
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

}
