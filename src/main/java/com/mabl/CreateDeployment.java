package com.mabl;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import com.mabl.domain.CreateDeploymentResult;
import com.mabl.domain.ExecutionResult;
import org.jetbrains.annotations.NotNull;

import static org.apache.commons.lang.StringUtils.isEmpty;

@Scanned
public class CreateDeployment implements TaskType {
    private I18nResolver i18nResolver;

    public CreateDeployment(@ComponentImport I18nResolver i18nResolver) {
        this.i18nResolver = i18nResolver;
    }

    @NotNull
    @Override
    public TaskResult execute(@NotNull TaskContext taskContext) {
        final BuildLogger buildLogger = taskContext.getBuildLogger();
        final String formApiKey = taskContext.getConfigurationMap().get("restApiKey");
        final String environmentId = taskContext.getConfigurationMap().get("environmentId");
        final String applicationId = taskContext.getConfigurationMap().get("applicationId");
        ExecutionResult executionResult;

        try (RestApiClient apiClient = new RestApiClient(MablConstants.MABL_REST_API_BASE_URL, formApiKey)) {

            CreateDeploymentResult deployment = apiClient.createDeploymentEvent(environmentId, applicationId);
            buildLogger.addBuildLogEntry(createLogLine(false,"Creating deployment with id '%s'", deployment.id));

            do {
                Thread.sleep(MablConstants.EXECUTION_STATUS_POLLING_INTERNAL_MILLISECONDS);
                executionResult = apiClient.getExecutionResults(deployment.id);

                if (executionResult == null) {
                    buildLogger.addErrorLogEntry(createLogLine(true,
                            "No deployment event found for id '%s' in mabl.",
                            deployment.id
                    ));

                    return TaskResultBuilder.newBuilder(taskContext).failed().build();
                }

                logAllJourneyExecutionStatuses(executionResult, buildLogger);

            } while (!allPlansComplete(executionResult));

        } catch (RuntimeException | InterruptedException e) {
            Thread.currentThread().interrupt();
            buildLogger.addErrorLogEntry(createLogLine(true, "Task Execution Exception: '%s'", e.getMessage()));
            return TaskResultBuilder.newBuilder(taskContext).failed().build();
        }

        if (!finalOutputStatusAllSuccesses(executionResult, buildLogger)) {
            buildLogger.addErrorLogEntry(createLogLine(true, "One or more plans were unsuccessful"));
            return TaskResultBuilder.newBuilder(taskContext).failed().build();
        }

        buildLogger.addBuildLogEntry(createLogLine(false, "All plans were successful."));
        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    private boolean finalOutputStatusAllSuccesses(final ExecutionResult result, final BuildLogger buildLogger) {
        boolean allPlansSuccess = true;
        buildLogger.addBuildLogEntry(createLogLine(false, "The final Plan states in mabl:"));
        for (ExecutionResult.ExecutionSummary summary : result.executions) {
            final String successState = summary.success ? "SUCCEEDED" : "FAILED";
            if(summary.success) {
                buildLogger.addBuildLogEntry(createLogLine(false,
                        "Plan '%s' has %s with state '%s'",
                        safePlanName(summary),
                        successState,
                        summary.status
                ));
            } else {
                allPlansSuccess = false;
                buildLogger.addErrorLogEntry(createLogLine(true,
                        "Plan '%s' has %s with state '%s'",
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
        buildLogger.addBuildLogEntry(createLogLine(false, "Running mabl journey(s) status update:"));
        if(result.executions.size() == 0) {
            buildLogger.addErrorLogEntry(createLogLine(true, "No executions exists for this plan."));
        }
        for (ExecutionResult.ExecutionSummary summary : result.executions) {
            logPlanExecutionStatuses(summary, buildLogger);
        }
    }

    private void logPlanExecutionStatuses(
            final ExecutionResult.ExecutionSummary planSummary,
            final BuildLogger buildLogger
    ) {
        buildLogger.addBuildLogEntry(createLogLine(false,
                "Plan '%s' is in state '%s'",
                safePlanName(planSummary),
                planSummary.status
        ));
        for (ExecutionResult.JourneyExecutionResult journeyResult : planSummary.journeyExecutions) {
            buildLogger.addBuildLogEntry(createLogLine(false,
                    "Journey '%s' is in state '%s'",
                    safeJourneyName(planSummary, journeyResult.id),
                    journeyResult.status
            ));
        }
    }

    private String safePlanName(final ExecutionResult.ExecutionSummary summary) {
        // Defensive treatment of possibly malformed future payloads
        return summary.plan != null && !isEmpty(summary.plan.name) ? summary.plan.name : "<Unnamed Plan>";
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

    private String createLogLine(boolean isError, String log) {
        return createLogLine(isError, log, new Object[0]);
    }

    private String createLogLine(boolean isError, String template, Object... args) {
        if(isError) {
            template = "ERROR: " + template;
        }

        String prefix = i18nResolver.getText("mabl.task.output.prefix");
        template = prefix+template;
        return String.format(template, args);
    }
}
