package com.mabl;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.mabl.domain.CreateDeploymentResult;
import com.mabl.domain.ExecutionResult;
import org.jetbrains.annotations.NotNull;

public class CreateDeployment implements TaskType {

    @NotNull
    @Override
    public TaskResult execute(@NotNull TaskContext taskContext) {
        final BuildLogger buildLogger = taskContext.getBuildLogger();
        final String formApiKey = taskContext.getConfigurationMap().get("restApiKey");
        final String environmentId = taskContext.getConfigurationMap().get("environmentId");
        final String applicationId = taskContext.getConfigurationMap().get("applicationId");
        ExecutionResult executionResult;

        try (RestApiClient apiClient = new RestApiClient(formApiKey)) {

            CreateDeploymentResult deployment = apiClient.createDeploymentEvent(environmentId, applicationId);
            buildLogger.addBuildLogEntry(String.format("Creating deployment with id '%s'", deployment.id));

            do {
                Thread.sleep(MablConstants.EXECUTION_STATUS_POLLING_INTERNAL_MILLISECONDS);
                executionResult = apiClient.getExecutionResults(deployment.id);

                if (executionResult == null) {
                    buildLogger.addErrorLogEntry(String.format(
                            "ERROR: No deployment event found for id '%s' in Mabl.",
                            deployment.id
                    ));

                    return TaskResultBuilder.newBuilder(taskContext).failed().build();
                }

                logAllJourneyExecutionStatuses(executionResult, buildLogger);

            } while (!allPlansComplete(executionResult));

        } catch (RuntimeException | InterruptedException e) {
            buildLogger.addErrorLogEntry(String.format("ERROR: Task Execution Exception: '%s'", e.getMessage()));
            return TaskResultBuilder.newBuilder(taskContext).failed().build();
        }

        if (!finalOutputStatusAllSuccesses(executionResult, buildLogger)) {
            buildLogger.addErrorLogEntry("ERROR: One or more plans were unsuccessful");
            return TaskResultBuilder.newBuilder(taskContext).failed().build();
        }

        buildLogger.addBuildLogEntry("All plans were successful.");
        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    private boolean finalOutputStatusAllSuccesses(final ExecutionResult result, final BuildLogger buildLogger) {
        boolean allPlansSuccess = true;
        buildLogger.addBuildLogEntry("The final Plan states in Mabl:");
        for (ExecutionResult.ExecutionSummary summary : result.executions) {
            final String successState = summary.success ? "SUCCEEDED" : "FAILED";
            if(summary.success) {
                buildLogger.addBuildLogEntry(String.format(
                        "Plan '%s' has %s with state '%s'",
                        safePlanName(summary),
                        successState,
                        summary.status
                ));
            } else {
                allPlansSuccess = false;
                buildLogger.addErrorLogEntry(String.format(
                        "ERROR: Plan '%s' has %s with state '%s'",
                        safePlanName(summary),
                        successState,
                        summary.status
                ));
            }
        }

        return allPlansSuccess;
    }

    private boolean allPlansComplete(final ExecutionResult result) {
        boolean isComplete = true;
        for (ExecutionResult.ExecutionSummary summary : result.executions) {
            isComplete &= MablConstants.COMPLETE_STATUSES.contains(summary.status.toLowerCase());
        }
        return isComplete;
    }

    private void logAllJourneyExecutionStatuses(final ExecutionResult result, final BuildLogger buildLogger) {
        buildLogger.addBuildLogEntry("Running Mabl journey(s) status update:");
        for (ExecutionResult.ExecutionSummary summary : result.executions) {
            logPlanExecutionStatuses(summary, buildLogger);
        }
    }

    private void logPlanExecutionStatuses(
            final ExecutionResult.ExecutionSummary planSummary,
            final BuildLogger buildLogger
    ) {
        buildLogger.addBuildLogEntry(String.format(
                "Plan '%s' is in state '%s'",
                safePlanName(planSummary),
                planSummary.status
        ));
        for (ExecutionResult.JourneyExecutionResult journeyResult : planSummary.journeyExecutions) {
            buildLogger.addBuildLogEntry(String.format(
                    "Journey '%s' is in state '%s'",
                    safeJourneyName(planSummary, journeyResult.id),
                    journeyResult.status
            ));
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
