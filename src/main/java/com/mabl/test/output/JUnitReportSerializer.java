package com.mabl.test.output;

import com.atlassian.extras.common.log.Logger;
import com.google.common.collect.ImmutableList;
import com.mabl.domain.ExecutionResult;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class JUnitReportSerializer {
    private static final Logger.Log log = Logger.getInstance(JUnitReportSerializer.class);

    private final File reportFile;
    private final List<ExecutionResult.ExecutionSummary> summaries;

    public JUnitReportSerializer(final File reportFile, final List<ExecutionResult.ExecutionSummary> summaries) {
        this.reportFile = reportFile;
        this.summaries = summaries;

    }

    public void generate() {
        List<TestSuite> suites = summaries.stream().map(suite -> new TestSuiteSerializer(suite).serialize()).collect(Collectors.toList());
        TestSuites testSuites = new TestSuites(ImmutableList.copyOf(suites));
        outputTestSuiteXml(testSuites);
    }

    private void outputTestSuiteXml(final TestSuites testSuites) {
        try {
            JAXBContext context = JAXBContext.newInstance(TestSuites.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(testSuites, reportFile);
        } catch (JAXBException e) {
            log.error("There was an error trying to output test results in mabl.", e);
        }
    }
}
