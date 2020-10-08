package com.mabl.test.output;

import com.atlassian.extras.common.log.Logger;
import com.mabl.domain.ExecutionResult;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import static com.mabl.Utils.safeJourneyName;
import static com.mabl.Utils.safePlanName;

public class TestSuiteSerializer {
    private static final Logger.Log LOG = Logger.getInstance(TestSuiteSerializer.class);

    private final ExecutionResult.ExecutionSummary summary;

    public TestSuiteSerializer(final ExecutionResult.ExecutionSummary summary) {
        this.summary = summary;
    }

    public TestSuite serialize() {
        final String timestamp = Instant.ofEpochMilli(summary.startTime).toString();
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
                        final SortedSet<String> newIds = new TreeSet<>();
                        for (ExecutionResult.TestCaseId id : journeyResult.testCases) {
                            newIds.add(id.caseId);
                        }
                        ids.addAll(newIds);

                        // XRay - report extension
                        // https://docs.getxray.app/display/XRAYCLOUD/Taking+advantage+of+JUnit+XML+reports
                        testCase.setTestCaseIDs(newIds);
                        break;
                    default:
                        // ignore, only the above statuses are captured
                }
            }

            testSuite.addToTestCases(testCase).incrementTests();

            if (!journeyResult.success && null != journeyResult.status) {
                switch (journeyResult.status) {
                    case "failed":
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
                        LOG.warn(String.format("unexpected status '%s' found for test '%s' in plan '%s'%n",
                                journeyResult.status,
                                planName,
                                testName));
                }

            }

        }

        if (!testCaseIDs.isEmpty()) {
            for (Map.Entry<String,SortedSet<String>> testCaseStatusEntry : testCaseIDs.entrySet()) {
                testSuite.addProperty(testCaseStatusEntry.getKey(), String.join(",", testCaseStatusEntry.getValue()));
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
