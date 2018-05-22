package com.mabl;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.google.common.collect.ImmutableSet;
import com.mabl.domain.CreateDeploymentResult;
import com.mabl.domain.ExecutionResult;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class CreateDeployment implements TaskType {
    private static final Set<String> COMPLETE_STATUSES = ImmutableSet.of(
            "succeeded",
            "failed",
            "cancelled",
            "completed",
            "terminated"
    );

    @NotNull
    @Override
    public TaskResult execute(@NotNull TaskContext taskContext) {
        final BuildLogger buildLogger = taskContext.getBuildLogger();
        final String formApiKey = taskContext.getConfigurationMap().get("restApiKey");
        final String environmentId = taskContext.getConfigurationMap().get("environmentId");
        final String applicationId = taskContext.getConfigurationMap().get("applicationId");

        RestApiClient apiClient = new RestApiClient(formApiKey);
        CreateDeploymentResult deployment = apiClient.createDeploymentEvent(environmentId, applicationId);
        buildLogger.addBuildLogEntry(String.format("Creating deployment with id '%s'", deployment.id));

        try {

            // Poll until we are successful or failed - note execution service is responsible for timeout
            ExecutionResult executionResult;
            do {
                Thread.sleep(10000);
                executionResult = apiClient.getExecutionResults(deployment.id);

                if (executionResult == null) {
                    // No such id - this shouldn't happen
                    throw new RuntimeException(String.format("No deployment event found for id [%s] in mabl.", deployment.id));
                }

                logAllJourneyExecutionStatuses(executionResult, buildLogger);

            } while (!allPlansComplete(executionResult));

            logFinalStatuses(executionResult, buildLogger);

            if (!allPlansSuccess(executionResult)) {
                buildLogger.addErrorLogEntry("ERROR: One or more plans were unsuccessful");
                return TaskResultBuilder.newBuilder(taskContext).failed().build();
            }

        } catch (InterruptedException e) {
            buildLogger.addErrorLogEntry(String.format("ERROR: Interrupted Exception: '%s'", e.getMessage()));
            return TaskResultBuilder.newBuilder(taskContext).failed().build();
        }

        buildLogger.addBuildLogEntry("All plans were successful.");
        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    private void logFinalStatuses(final ExecutionResult result, final BuildLogger buildLogger) {

        buildLogger.addBuildLogEntry("The final Plan states in mabl:");
        for (ExecutionResult.ExecutionSummary summary : result.executions) {
            final String successState = summary.success ? "SUCCESSFUL" : "FAILED";
            if(summary.success) {
                buildLogger.addBuildLogEntry(String.format("Plan [%s] is %s in state [%s]%n", safePlanName(summary), successState, summary.status));
            } else {
                buildLogger.addErrorLogEntry(String.format("ERROR: Plan [%s] is %s in state [%s]%n", safePlanName(summary), successState, summary.status));
            }
        }
    }

    private boolean allPlansComplete(final ExecutionResult result) {

        boolean isComplete = true;

        for (ExecutionResult.ExecutionSummary summary : result.executions) {
            isComplete &= COMPLETE_STATUSES.contains(summary.status.toLowerCase());
        }
        return isComplete;
    }

    private boolean allPlansSuccess(final ExecutionResult result) {

        boolean isSuccess = true;

        for (ExecutionResult.ExecutionSummary summary : result.executions) {
            isSuccess &= summary.success;
        }
        return isSuccess;
    }

    private void logAllJourneyExecutionStatuses(final ExecutionResult result, final BuildLogger buildLogger) {

        buildLogger.addBuildLogEntry("Running mabl journey(s) status update:");
        for (ExecutionResult.ExecutionSummary summary : result.executions) {
            buildLogger.addBuildLogEntry(String.format("Plan [%s] is [%s]%n", safePlanName(summary), summary.status));
            for (ExecutionResult.JourneyExecutionResult journeyResult : summary.journeyExecutions) {
                buildLogger.addBuildLogEntry(String.format("Journey [%s] is [%s]%n", safeJourneyName(summary, journeyResult.id), journeyResult.status));
            }
        }
    }

    private String safePlanName(final ExecutionResult.ExecutionSummary summary) {
        // Defensive treatment of possibly malformed future payloads
        return summary.plan != null &&
                summary.plan.name != null &&
                !summary.plan.name.isEmpty()
                ? summary.plan.name :
                "<Unnamed Plan>";
    }

    private String safeJourneyName(
            final ExecutionResult.ExecutionSummary summary,
            final String journeyId
    ) {
        // Defensive treatment of possibly malformed future payloads
        String journeyName = "<Unnamed Journey>";
        for(ExecutionResult.JourneySummary journeySummary: summary.journeys) {
            if(journeySummary.id.equals(journeyId) && !journeySummary.name.isEmpty()) {
                journeyName = journeySummary.name;
                break;
            }
        }

        return journeyName;
    }
}
