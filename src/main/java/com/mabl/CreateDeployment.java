package com.mabl;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.build.test.TestCollationService;
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

@Scanned
public class CreateDeployment implements TaskType {
    private final I18nResolver i18nResolver;
    private final TestCollationService testCollationService;

    public CreateDeployment(
            @ComponentImport I18nResolver i18nResolver,
            @ComponentImport TestCollationService testCollationService
    ) {
        this.i18nResolver = i18nResolver;
        this.testCollationService = testCollationService;
    }

    @NotNull
    @Override
    public TaskResult execute(@NotNull TaskContext taskContext) {
        final BuildLogger buildLogger = taskContext.getBuildLogger();
        final TaskResultBuilder taskResultBuilder = TaskResultBuilder.newBuilder(taskContext);
        final MablOutputProvider mablOutputProvider = new MablOutputProvider();
        final String formApiKey = taskContext.getConfigurationMap().get("restApiKey");
        final String environmentId = taskContext.getConfigurationMap().get("environmentId");
        final String applicationId = taskContext.getConfigurationMap().get("applicationId");
        ExecutionResult executionResult;

        try (RestApiClient apiClient = new RestApiClient(MablConstants.MABL_REST_API_BASE_URL, formApiKey)) {

            CreateDeploymentResult deployment = apiClient.createDeploymentEvent(environmentId, applicationId);
            buildLogger.addBuildLogEntry(createLogLine("Creating deployment with id '%s'", deployment.id));

            do {
                Thread.sleep(MablConstants.EXECUTION_STATUS_POLLING_INTERNAL_MILLISECONDS);
                executionResult = apiClient.getExecutionResults(deployment.id);

                if (executionResult == null) {
                    buildLogger.addErrorLogEntry(createLogErrorLine(
                            "No deployment event found for id '%s' in Mabl.",
                            deployment.id
                    ));

                    return taskResultBuilder.failed().build();
                }

                logAllJourneyExecutionStatuses(executionResult, buildLogger);

            } while (!allPlansComplete(executionResult));

        } catch (RuntimeException | InterruptedException e) {
            buildLogger.addErrorLogEntry(createLogErrorLine("Task Execution Exception: '%s'", e.getMessage()));
            return taskResultBuilder.failed().build();
        }

        if (!finalOutputStatusAllSuccesses(executionResult, buildLogger, mablOutputProvider)) {
            buildLogger.addErrorLogEntry(createLogErrorLine("One or more plans were unsuccessful"));
        } else {
            buildLogger.addBuildLogEntry(createLogLine("All plans were successful."));
        }

        testCollationService.collateTestResults(taskContext, mablOutputProvider);
        return taskResultBuilder.checkTestFailures().build();
    }

    private boolean finalOutputStatusAllSuccesses(
            final ExecutionResult result,
            final BuildLogger buildLogger,
            final MablOutputProvider outputProvider
    ) {
        boolean allPlansSuccess = true;
        buildLogger.addBuildLogEntry(createLogLine("The final Plan states in Mabl:"));
        for (ExecutionResult.ExecutionSummary summary : result.executions) {
            final String successState = summary.success ? "SUCCEEDED" : "FAILED";
            if(summary.success) {
                buildLogger.addBuildLogEntry(createLogLine(
                        "Plan '%s' has %s with state '%s'",
                        safePlanName(summary),
                        successState,
                        summary.status
                ));
            } else {
                allPlansSuccess = false;
                buildLogger.addErrorLogEntry(createLogErrorLine(
                        "Plan '%s' has %s with state '%s'",
                        safePlanName(summary),
                        successState,
                        summary.status
                ));
            }

            for(ExecutionResult.JourneyExecutionResult journeyResult : summary.journeyExecutions) {
                long duration = summary.stopTime-summary.startTime;
                if(journeyResult.success) {
                    outputProvider.addSuccess(safePlanName(summary), safeJourneyName(summary, journeyResult.id), duration);
                } else {
                    outputProvider.addFailure(safePlanName(summary), safeJourneyName(summary, journeyResult.id), duration);
                }
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
        buildLogger.addBuildLogEntry(createLogLine("Running Mabl journey(s) status update:"));
        if(result.executions.isEmpty()) {
            buildLogger.addErrorLogEntry(createLogErrorLine("No executions exists for this plan."));
        }
        for (ExecutionResult.ExecutionSummary summary : result.executions) {
            logPlanExecutionStatuses(summary, buildLogger);
        }
    }

    private void logPlanExecutionStatuses(
            final ExecutionResult.ExecutionSummary planSummary,
            final BuildLogger buildLogger
    ) {
        buildLogger.addBuildLogEntry(createLogLine(
                "Plan '%s' is in state '%s'",
                safePlanName(planSummary),
                planSummary.status
        ));
        for (ExecutionResult.JourneyExecutionResult journeyResult : planSummary.journeyExecutions) {
            buildLogger.addBuildLogEntry(createLogLine(
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

    private String createLogErrorLine(String template, Object... args) {
        return createLogHelper(true, template, args);
    }

    private String createLogLine(String logline) {
        return createLogHelper(false, logline, new Object[0]);
    }

    private String createLogLine(String template, Object... args) {
        return createLogHelper(false, template, args);
    }

    private String createLogHelper(boolean isError, String template, Object... args) {
        if(isError) {
            template = "ERROR: " + template;
        }

        String prefix = i18nResolver.getText("mabl.task.output.prefix");
        template = prefix+template;
        return String.format(template, args);
    }
}
