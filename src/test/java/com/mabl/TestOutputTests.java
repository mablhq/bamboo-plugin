package com.mabl;

import com.google.common.collect.ImmutableList;
import com.mabl.test.output.Failure;
import com.mabl.test.output.Properties;
import com.mabl.test.output.Property;
import com.mabl.test.output.TestCase;
import com.mabl.test.output.TestSuite;
import com.mabl.test.output.TestSuites;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static com.mabl.TestUtils.readFileAsString;

public class TestOutputTests {

    @Test
    public void testTestCaseOutputNoFailure() throws JAXBException {
        TestCase testCase = new TestCase("My Plan Name", "My Test Name", 23L, "https://app.mabl.com");
        JAXBContext jaxbContext = JAXBContext.newInstance(TestCase.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter stringWriter = new StringWriter();
        marshaller.marshal(testCase, stringWriter);

        assertEquals(stringWriter.toString(), readFileAsString("testcase.xml"));
    }

    @Test
    public void testTestCaseOutputWithFailure() throws JAXBException {
        Failure failure = new Failure("My Reason", "My Message");
        TestCase testCase = new TestCase("My Plan Name", "My Test Name", 23L, "https://app.mabl.com", failure);
        JAXBContext jaxbContext = JAXBContext.newInstance(TestCase.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter stringWriter = new StringWriter();
        marshaller.marshal(testCase, stringWriter);
        assertEquals(stringWriter.toString(), readFileAsString("testcasewithfailure.xml"));
    }

    @Test
    public void testEntireTestSuite() throws JAXBException {
        TestSuite emptyTestSuite = new TestSuite(
                "Empty Test Suite",
                0L,
                "2013-05-24T10:23:58",
                new Properties()
        );


        List<Property> props = Arrays.asList(
                new Property("environment", "my env-e"),
                new Property("application", "my app-a"),
                new Property("completed-test-cases", "TESTCASE-1"),
                new Property("failed-test-cases", "TESTCASE-2"),
                new Property("skipped-test-cases", "TESTCASE-3")
        );
        Properties properties = new Properties(ImmutableList.copyOf(props));

        TestCase testCase1 = new TestCase("My Plan Name 1", "My Test Name 1", 11L, "https://app.mabl.com");
        testCase1.setTestCaseIDs(Collections.singleton("TESTCASE-1"));
        Failure failure = new Failure("My Reason", "My Message");
        TestCase testCase2 = new TestCase("My Plan Name 2", "My Test Name 2", 22L, "https://app.mabl.com", failure);
        testCase2.setTestCaseIDs(Collections.singleton("TESTCASE-2"));
        TestCase testCase3 = new TestCase("My Plan Name 3", "My Test Name 3", 33L, "https://app.mabl.com");
        testCase3.setSkipped();
        testCase3.setTestCaseIDs(Collections.singleton("TESTCASE-3"));

        TestSuite testSuite1 = new TestSuite(
                "Full Test Suite",
                33L,
                "2013-05-24T10:23:58",
                properties
        );
        testSuite1.addToTestCases(testCase1);
        testSuite1.incrementTests();
        testSuite1.addToTestCases(testCase2);
        testSuite1.incrementTests();
        testSuite1.incrementFailures();
        testSuite1.addToTestCases(testCase3);
        testSuite1.incrementTests();
        testSuite1.incrementSkipped();

        ArrayList<TestSuite> suites = new ArrayList<>();
        suites.add(emptyTestSuite);
        suites.add(testSuite1);
        TestSuites testSuites = new TestSuites(ImmutableList.copyOf(suites));

        JAXBContext jaxbContext = JAXBContext.newInstance(TestSuites.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter stringWriter = new StringWriter();
        marshaller.marshal(testSuites, stringWriter);
        assertEquals(stringWriter.toString(), readFileAsString("testsuite.xml"));
    }


}
