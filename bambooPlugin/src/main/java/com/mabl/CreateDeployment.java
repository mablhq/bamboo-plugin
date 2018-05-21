package com.mabl;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.mabl.domain.CreateDeploymentResult;
import org.jetbrains.annotations.NotNull;

public class CreateDeployment implements TaskType {
    private RestApiClient apiClient;

    public CreateDeployment() {
        this.apiClient = new RestApiClient();
    }

    @NotNull
    @Override
    public TaskResult execute(@NotNull TaskContext taskContext) {
        final BuildLogger buildLogger = taskContext.getBuildLogger();
        final String formApiKey = taskContext.getConfigurationMap().get("restApiKey");
        final String environmentId = taskContext.getConfigurationMap().get("environmentId");
        final String applicationId = taskContext.getConfigurationMap().get("applicationId");

        CreateDeploymentResult deployment = apiClient.createDeploymentEvent(formApiKey, environmentId, applicationId);
        buildLogger.addBuildLogEntry(String.format("Creating deployment with id %d", deployment.id));
        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }
}
