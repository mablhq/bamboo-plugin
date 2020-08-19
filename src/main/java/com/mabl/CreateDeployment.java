package com.mabl;

import com.atlassian.bamboo.artifact.Artifact;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.build.test.TestCollationService;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.bamboo.variable.VariableDefinitionContext;
import com.atlassian.extras.common.log.Logger;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.collect.ImmutableList;
import com.mabl.domain.CreateDeploymentProperties;
import com.mabl.domain.CreateDeploymentResult;
import com.mabl.domain.ExecutionResult;
import com.mabl.test.output.Failure;
import com.mabl.test.output.TestCase;
import com.mabl.test.output.TestSuite;
import com.mabl.test.output.TestSuites;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import static com.mabl.MablConstants.APPLICATION_ID_FIELD;
import static com.mabl.MablConstants.COMPLETE_STATUSES;
import static com.mabl.MablConstants.ENVIRONMENT_ID_FIELD;
import static com.mabl.MablConstants.EXECUTION_STATUS_POLLING_INTERNAL_MILLISECONDS;
import static com.mabl.MablConstants.MABL_LOG_OUTPUT_PREFIX;
import static com.mabl.MablConstants.MABL_REST_API_BASE_URL;
import static com.mabl.MablConstants.PLAN_LABELS_FIELD;
import static com.mabl.MablConstants.REST_API_KEY_FIELD;
import static org.apache.commons.lang.StringUtils.isEmpty;

@Scanned
public class CreateDeployment implements TaskType {
    private static final Logger.Log log = Logger.getInstance(CreateDeployment.class);

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
        final File junitReport = new File(taskContext.getWorkingDirectory(), "report.xml");
        final BuildLogger buildLogger = taskContext.getBuildLogger();
        final TaskResultBuilder taskResultBuilder = TaskResultBuilder.newBuilder(taskContext);
        final MablOutputProvider mablOutputProvider = new MablOutputProvider();
        final String formApiKey = taskContext.getConfigurationMap().get(REST_API_KEY_FIELD);
        final String environmentId = taskContext.getConfigurationMap().get(ENVIRONMENT_ID_FIELD);
        final String applicationId = taskContext.getConfigurationMap().get(APPLICATION_ID_FIELD);
        final String labels = taskContext.getConfigurationMap().get(PLAN_LABELS_FIELD);

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

        try (RestApiClient apiClient = new RestApiClient(MABL_REST_API_BASE_URL, formApiKey)) {

            CreateDeploymentResult deployment = apiClient.createDeploymentEvent(environmentId, applicationId, planLabels, properties);
            buildLogger.addBuildLogEntry(
                    createLogLine(
                            "Created deployment at https://app.mabl.com/workspaces/%s/events/%s and triggered '%d' plans.",
                            deployment.workspaceId, deployment.id, deployment.triggeredPlanRunSummaries.size()));
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

        generateJUnitReport(junitReport, executionResult.executions);

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

        return "true".equalsIgnoreCase(context.get(MablConstants.MABL_SEND_VARIABLES_FIELD).getValue());
    }

    private boolean finalOutputStatusAllSuccesses(
            final ExecutionResult result,
            final BuildLogger buildLogger,
            final MablOutputProvider outputProvider
    ) {

        buildLogger.addBuildLogEntry(createLogLine("The final Plan states in Mabl:"));
        final Set<String> retriedPlanIDs = new HashSet<>();
        result.executions.stream().
                filter(summary -> (summary.planExecution != null && summary.planExecution.isRetry)).
                filter(summary -> (summary.plan != null && summary.plan.id != null)).
                forEach(summary -> retriedPlanIDs.add(summary.plan.id));

        for (ExecutionResult.ExecutionSummary summary : result.executions) {
            final String successState = summary.success ? "SUCCEEDED" : "FAILED";
            if (summary.success || (summary.planExecution != null &&
                            !summary.planExecution.isRetry &&
                            summary.plan != null &&
                            summary.plan.id != null &&
                            retriedPlanIDs.contains(summary.plan.id))) {
                buildLogger.addBuildLogEntry(createLogLine(
                        "%sPlan '%s' has %s with state '%s'",
                        (summary.planExecution != null && summary.planExecution.isRetry) ? "RETRY: " : "",
                        safePlanName(summary),
                        successState,
                        summary.status
                ));
            } else {
                buildLogger.addErrorLogEntry(createLogErrorLine(
                        "%sPlan '%s' has %s with state '%s'",
                        (summary.planExecution != null && summary.planExecution.isRetry) ? "RETRY: " : "",
                        safePlanName(summary),
                        successState,
                        summary.status
                ));
            }

            for (ExecutionResult.JourneyExecutionResult journeyResult : summary.journeyExecutions) {
                final long duration = journeyResult.stopTime - journeyResult.startTime;
                final String planName = safePlanName(summary);
                final String testName = safeJourneyName(summary, journeyResult.id);
                if (journeyResult.success) {
                    outputProvider.addSuccess(planName, testName, duration);
                } else if ("skipped".equals(journeyResult.status)) {
                    outputProvider.addSkipped(planName, testName);
                } else {
                    // suppress logging test run failures that are retries (i.e. only log the failure if the retry
                    // failed), otherwise Bamboo will consider this as a plan failure.
                    if (summary.planExecution == null ||
                            summary.planExecution.isRetry ||
                            summary.plan == null ||
                            summary.plan.id == null ||
                            !retriedPlanIDs.contains(summary.plan.id)
                    ) {
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
                (planSummary.planExecution != null && planSummary.planExecution.isRetry) ? "RETRY: " : "",
                safePlanName(planSummary),
                planSummary.status
        ));
        for (ExecutionResult.JourneyExecutionResult journeyResult : planSummary.journeyExecutions) {
            buildLogger.addBuildLogEntry(createLogLine(
                    "    Test '%s' is in state '%s'",
                    safeJourneyName(planSummary, journeyResult.id),
                    journeyResult.status
            ));
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

    private static void generateJUnitReport(final File reportFile, final List<ExecutionResult.ExecutionSummary> summaries) {
        List<TestSuite> suites = new ArrayList<>();
        summaries.forEach(summary -> suites.add(createTestSuite(summary)));
        TestSuites testSuites = new TestSuites(ImmutableList.copyOf(suites));
        outputTestSuiteXml(reportFile, testSuites);
    }

    private static void outputTestSuiteXml(final File reportFile, final TestSuites testSuites) {
        try {
            JAXBContext context = JAXBContext.newInstance(TestSuites.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(testSuites, reportFile);
        } catch (JAXBException e) {
            log.error("There was an error trying to output test results in mabl.", e);
        }
    }

    private static TestSuite createTestSuite(final ExecutionResult.ExecutionSummary summary) {
        final Date startDate = new Date(summary.startTime);
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String timestamp = format.format(startDate);
        final String planName = safePlanName(summary);
        final TestSuite testSuite = new TestSuite(planName, getDuration(summary), timestamp);

        final Map<String, SortedSet<String>> testCaseIDs = new HashMap<>();

        for (ExecutionResult.JourneyExecutionResult journeyResult : summary.journeyExecutions) {
            final String testName = safeJourneyName(summary, journeyResult.id);
            TestCase testCase = new TestCase(
                    planName,
                    testName,
                    getDuration(journeyResult),
                    journeyResult.appHref
            );

            if (journeyResult.testCases != null && !journeyResult.testCases.isEmpty()) {
                switch (journeyResult.status) {
                    case "failed":
                    case "completed":
                    case "skipped":
                        final SortedSet<String> ids =
                                testCaseIDs.computeIfAbsent(journeyResult.status + "-test-cases", k -> new TreeSet<>());
                        for (ExecutionResult.TestCaseID id : journeyResult.testCases) {
                            ids.add(id.caseID);
                        }

                        // XRay - report extension
                        // https://docs.getxray.app/display/XRAYCLOUD/Taking+advantage+of+JUnit+XML+reports
                        testCase.setTestCaseIDs(ids);
                        break;
                    default:
                        // ignore, only the above statuses are captured
                }
            }

            testSuite.addToTestCases(testCase).incrementTests();

            if (!journeyResult.success && null != journeyResult.status) {
                switch (journeyResult.status) {
                    case "failed":
                        // fall through
                    case "terminated":
                        final Failure failure = new Failure(journeyResult.status, journeyResult.statusCause);
                        testCase.setFailure(failure);
                        testSuite.incrementFailures();
                        break;
                    case "skipped":
                        testCase.setSkipped();
                        testSuite.incrementSkipped();
                        break;
                    default:
                        log.warn(String.format("unexpected status '%s' found for test '%s' in plan '%s'%n",
                                journeyResult.status,
                                planName,
                                testName));
                }

            }

        }

        if (!testCaseIDs.isEmpty()) {
            for (Map.Entry<String,SortedSet<String>> e : testCaseIDs.entrySet()) {
                testSuite.addProperty(e.getKey(), String.join(",", e.getValue()));
            }
        }
        return testSuite;
    }

    private static long getDuration(ExecutionResult.ExecutionSummary summary) {
        return summary.stopTime != null ?
                TimeUnit.SECONDS.convert( (summary.stopTime - summary.startTime), TimeUnit.MILLISECONDS) : 0;
    }

    private static long getDuration(ExecutionResult.JourneyExecutionResult summary) {
        return summary.stopTime != null ?
                TimeUnit.SECONDS.convert( (summary.stopTime - summary.startTime), TimeUnit.MILLISECONDS) : 0;
    }

}
