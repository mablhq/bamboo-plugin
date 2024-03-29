package com.mabl;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.build.test.TestCollationService;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.bamboo.variable.VariableDefinitionContext;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.mabl.domain.CreateDeploymentProperties;
import com.mabl.domain.CreateDeploymentResult;
import com.mabl.domain.ExecutionResult;
import com.mabl.test.output.JUnitReportSerializer;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.mabl.MablConstants.APPLICATION_ID_FIELD;
import static com.mabl.MablConstants.MABL_BRANCH_FIELD;
import static com.mabl.MablConstants.COMPLETE_STATUSES;
import static com.mabl.MablConstants.ENVIRONMENT_ID_FIELD;
import static com.mabl.MablConstants.EXECUTION_STATUS_POLLING_INTERNAL_MILLISECONDS;
import static com.mabl.MablConstants.MABL_JUNIT_REPORT_XML;
import static com.mabl.MablConstants.MABL_LOG_OUTPUT_PREFIX;
import static com.mabl.MablConstants.MABL_REST_API_BASE_URL;
import static com.mabl.MablConstants.PLAN_LABELS_FIELD;
import static com.mabl.MablConstants.PROXY_ADDRESS_FIELD;
import static com.mabl.MablConstants.PROXY_PASSWORD_FIELD;
import static com.mabl.MablConstants.PROXY_USERNAME_FIELD;
import static com.mabl.MablConstants.REST_API_KEY_FIELD;
import static org.apache.commons.lang.StringUtils.isEmpty;

@Scanned
public class CreateDeployment implements TaskType {
    private final TestCollationService testCollationService;
    private final CustomVariableContext customVariableContext;

    public CreateDeployment(
            @ComponentImport TestCollationService testCollationService,
            @ComponentImport CustomVariableContext customVariableContext
    ) {
        this.testCollationService = testCollationService;
        this.customVariableContext = customVariableContext;
    }

    @NotNull
    @Override
    public TaskResult execute(@NotNull TaskContext taskContext) {
        final File junitReport = new File(taskContext.getWorkingDirectory(), MABL_JUNIT_REPORT_XML);
        final BuildLogger buildLogger = taskContext.getBuildLogger();
        final TaskResultBuilder taskResultBuilder = TaskResultBuilder.newBuilder(taskContext);
        final MablOutputProvider mablOutputProvider = new MablOutputProvider();
        final String formApiKey = taskContext.getConfigurationMap().get(REST_API_KEY_FIELD);
        final String environmentId = taskContext.getConfigurationMap().get(ENVIRONMENT_ID_FIELD);
        final String applicationId = taskContext.getConfigurationMap().get(APPLICATION_ID_FIELD);
        final String labels = taskContext.getConfigurationMap().get(PLAN_LABELS_FIELD);
        final String mablBranch = taskContext.getConfigurationMap().get(MABL_BRANCH_FIELD);
        final String proxyAddress = taskContext.getConfigurationMap().get(PROXY_ADDRESS_FIELD);
        final String proxyUsername = taskContext.getConfigurationMap().get(PROXY_USERNAME_FIELD);
        final String proxyPassword = taskContext.getConfigurationMap().get(PROXY_PASSWORD_FIELD);

        Set<String> planLabels = new HashSet<>();
        if(!labels.isEmpty()) {
            planLabels.addAll(Arrays.asList(labels.split(",")));
        }
        final boolean sendEnvVars = getSendEnvVarsValue();
        final CreateDeploymentProperties properties = getMablProperties(sendEnvVars);
        buildLogger.addBuildLogEntry(createLogLine("'%s' is set to '%b'. Sending the following properties: '%s'",
                MablConstants.MABL_SEND_VARIABLES_FIELD,
                sendEnvVars,
                properties.toString()
        ));
        ExecutionResult executionResult;
        final ProxyConfiguration proxyConfig = new ProxyConfiguration(proxyAddress, proxyUsername, proxyPassword);
        try (RestApiClient apiClient = new RestApiClient(MABL_REST_API_BASE_URL, formApiKey, proxyConfig)) {

            CreateDeploymentResult deployment = apiClient.createDeploymentEvent(
                    environmentId, applicationId, planLabels, mablBranch, properties);
            buildLogger.addBuildLogEntry(
                    createLogLine(
                            "Created deployment at https://app.mabl.com/workspaces/%s/events/%s",
                            deployment.workspaceId, deployment.id));
            // Share the deployment event identifier with subsequent tasks
            customVariableContext.addCustomData("mabl.deployment.id", deployment.id);
            do {
                TimeUnit.MILLISECONDS.sleep(EXECUTION_STATUS_POLLING_INTERNAL_MILLISECONDS);
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
            Thread.currentThread().interrupt();
            buildLogger.addErrorLogEntry(createLogErrorLine("Task Execution Exception: '%s'", e.getMessage()));
            return taskResultBuilder.failed().build();
        }

        if (!finalOutputStatusAllSuccesses(executionResult, buildLogger, mablOutputProvider)) {
            buildLogger.addErrorLogEntry(createLogErrorLine("One or more plans were unsuccessful"));
        } else {
            buildLogger.addBuildLogEntry(createLogLine("All plans were successful."));
        }

        new JUnitReportSerializer(junitReport, executionResult.executions).generate();

        testCollationService.collateTestResults(taskContext, mablOutputProvider);
        return taskResultBuilder.checkTestFailures().build();
    }

     private CreateDeploymentProperties getMablProperties(boolean sendEnvVars) {
        CreateDeploymentProperties properties = new CreateDeploymentProperties();
        if(sendEnvVars) {
            properties = Converter
                    .customVariableContextToCreateDeploymentProperties.apply(this.customVariableContext);
        }
        properties.setDeploymentOrigin(MablConstants.PLUGIN_USER_AGENT);
        return properties;
    }

    // You can set a Bamboo plan level variable `mabl.sendvariables` that will enable the plugin to send additional
    // environment variables to mabl.
    private boolean getSendEnvVarsValue() {
        Map<String, VariableDefinitionContext> context = this.customVariableContext.getVariableContexts();
        if (!context.containsKey(MablConstants.MABL_SEND_VARIABLES_FIELD)) {
            return false;
        }

        return Boolean.parseBoolean(context.get(MablConstants.MABL_SEND_VARIABLES_FIELD).getValue());
    }

    private boolean finalOutputStatusAllSuccesses(
            final ExecutionResult result,
            final BuildLogger buildLogger,
            final MablOutputProvider outputProvider
    ) {

        buildLogger.addBuildLogEntry(createLogLine("The final Plan states in Mabl:"));
        final Set<String> retriedPlanIDs = new HashSet<>();
        result.executions.stream().
                filter(CreateDeployment::isRetriedPlanExecution).
                filter(summary -> (summary.plan != null && summary.plan.id != null)).
                forEach(summary -> retriedPlanIDs.add(summary.plan.id));

        for (ExecutionResult.ExecutionSummary summary : result.executions) {

            final boolean retriedPlanExecution = isRetriedPlanExecution(summary);
            final boolean retriedPlan = isRetriedPlan(summary, retriedPlanIDs);

            if (summary.success || (retriedPlan && !retriedPlanExecution)) {
                buildLogger.addBuildLogEntry(formatPlanLog(false, summary));
            } else {
                buildLogger.addErrorLogEntry(formatPlanLog(true, summary));
            }

            for (ExecutionResult.TestRunResult testRunResult : summary.testRunResults) {
                Long duration = null;
                if (testRunResult.stopTime != null && testRunResult.startTime != null) {
                    duration = testRunResult.stopTime - testRunResult.startTime;
                }
                final String planName = safePlanName(summary);
                String testName = safeJourneyName(summary, testRunResult.id);
                testName = maybeAppendDataTableScenarioName(testName, testRunResult);
                if (testRunResult.success) {
                    outputProvider.addSuccess(planName, testName, duration);
                } else if ("skipped".equals(testRunResult.status)) {
                    outputProvider.addSkipped(planName, testName);
                } else {
                    // suppress logging test run failures that are retries (i.e. only log the failure if the retry
                    // failed), otherwise Bamboo will consider this as a plan failure.
                    if (retriedPlanExecution || !retriedPlan) {
                        outputProvider.addFailure(planName, testName, duration);
                    }
                }
            }
        }

        return (result.eventStatus != null && result.eventStatus.succeeded != null && result.eventStatus.succeeded);
    }

    private boolean allPlansComplete(final ExecutionResult result) {
        boolean isComplete = true;
        for (ExecutionResult.ExecutionSummary summary : result.executions) {
            isComplete &= COMPLETE_STATUSES.contains(summary.status.toLowerCase());
        }
        return isComplete;
    }

    private void logAllJourneyExecutionStatuses(final ExecutionResult result, final BuildLogger buildLogger) {
        buildLogger.addBuildLogEntry(createLogLine("Running Mabl test(s) status update:"));
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
                "  %sPlan '%s' is in state '%s'",
                maybeRetryPrefix(planSummary),
                safePlanName(planSummary),
                planSummary.status
        ));
        for (ExecutionResult.TestRunResult testRunResult : planSummary.testRunResults) {
            Optional.ofNullable(testRunResult.status).ifPresent(status ->
                    buildLogger.addBuildLogEntry(createLogLine(
                            "    Test '%s' is in state '%s'",
                            safeJourneyName(planSummary, testRunResult.id),
                            status))
            );
        }
    }

    private static String safePlanName(final ExecutionResult.ExecutionSummary summary) {
        // Defensive treatment of possibly malformed future payloads
        return summary.plan != null && !isEmpty(summary.plan.name) ? summary.plan.name : "<Unnamed Plan>";
    }

    private static String safeJourneyName(
            final ExecutionResult.ExecutionSummary summary,
            final String journeyId
    ) {
        // Defensive treatment of possibly malformed future payloads
        String journeyName = "<Unnamed Test>";
        for(ExecutionResult.JourneySummary journeySummary: summary.journeys) {
            if(journeySummary.id.equals(journeyId) && !journeySummary.name.isEmpty()) {
                journeyName = journeySummary.name;
                break;
            }
        }

        return journeyName;
    }

    private static String maybeAppendDataTableScenarioName(final String testName, final ExecutionResult.TestRunResult testRunResult) {
        if (testRunResult.scenarioName == null) {
            return testName;
        }
        return testName + " (Scenario: " + testRunResult.scenarioName + ")";
    }

    private String createLogErrorLine(String template, Object... args) {
        return createLogHelper(true, template, args);
    }

    private String createLogLine(String logline) {
        return createLogHelper(false, logline);
    }

    private String createLogLine(String template, Object... args) {
        return createLogHelper(false, template, args);
    }

    private String createLogHelper(boolean isError, String template, Object... args) {
        if(isError) {
            template = "ERROR: " + template;
        }

        return String.format(MABL_LOG_OUTPUT_PREFIX+template, args);
    }

    private static boolean isRetriedPlan(ExecutionResult.ExecutionSummary summary, Collection<String> retriedPlanIDs) {
        return summary.plan != null &&  summary.plan.id != null && retriedPlanIDs.contains(summary.plan.id);
    }

    private static boolean isRetriedPlanExecution(ExecutionResult.ExecutionSummary summary) {
        return summary.planExecution != null && summary.planExecution.isRetry;
    }

    private static String maybeRetryPrefix(ExecutionResult.ExecutionSummary summary) {
        return isRetriedPlanExecution(summary) ? "RETRY" : "";
    }

    private String formatPlanLog(boolean isError, ExecutionResult.ExecutionSummary summary) {
        final String successState = summary.success ? "SUCCEEDED" : "FAILED";
        final String planName = safePlanName(summary);

        return createLogHelper(isError,
                "%sPlan '%s' has %s with state '%s'",
                maybeRetryPrefix(summary),
                planName,
                successState,
                summary.status
        );
    }

}
