package com.mabl;

import com.atlassian.bamboo.build.test.TestCollectionResult;
import com.atlassian.bamboo.build.test.TestCollectionResultBuilder;
import com.atlassian.bamboo.build.test.TestReportProvider;
import com.atlassian.bamboo.results.tests.TestResults;
import com.atlassian.bamboo.resultsummary.tests.TestState;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class MablOutputProvider implements TestReportProvider {
    public Collection<TestResults> successfulTestResults = Lists.newArrayList();
    public Collection<TestResults> failedTestResults = Lists.newArrayList();
    public Collection<TestResults> skippedTestResults = Lists.newArrayList();

    @Override
    @NotNull
    public TestCollectionResult getTestCollectionResult() {
        TestCollectionResultBuilder builder = new TestCollectionResultBuilder();

        return builder.addSuccessfulTestResults(successfulTestResults)
                .addFailedTestResults(failedTestResults)
                .addSkippedTestResults(skippedTestResults)
                .build();
    }

    public boolean addSuccess(TestResults testResults) {
        testResults.setState(TestState.SUCCESS);
        return successfulTestResults.add(testResults);
    }

    public boolean addSuccess(String className, String methodName, long duration) {
        return addSuccess(new TestResults(className, methodName, duration));
    }

    public boolean addFailure(TestResults testResults) {
        testResults.setState(TestState.FAILED);
        return failedTestResults.add(testResults);
    }

    public boolean addFailure(String className, String methodName, long duration) {
        return addFailure(new TestResults(className, methodName, duration));
    }

    public boolean addSkipped(TestResults testResults) {
        testResults.setState(TestState.SKIPPED);
        return skippedTestResults.add(testResults);
    }

    public boolean addSkipped(String className, String methodName) {
        return addSkipped(new TestResults(className, methodName, 0L));
    }
}
