package com.mabl;

import com.mabl.domain.ExecutionResult;
import com.mabl.test.output.JUnitReportSerializer;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.junit.Test;


import java.io.File;
import java.io.IOException;

import static com.mabl.TestUtils.fileEquals;
import static com.mabl.TestUtils.readFileAsString;
import static org.junit.Assert.assertTrue;
import static com.mabl.Utils.getObjectMapperSingleton;

public class JUnitReportSerializerTest {


    @Test
    public void testJsonToJunit() throws IOException {
        {
            // Arrange
            String executionResult = readFileAsString("result.json");
            HttpEntity httpEntity = new StringEntity(executionResult, ContentType.APPLICATION_JSON);
            HttpResponse response =
                    new BasicHttpResponse(
                            new ProtocolVersion("http", 1, 1), 200, "Success");
            response.setEntity(httpEntity);

            ExecutionResult result =
                    getObjectMapperSingleton().readerFor(ExecutionResult.class).readValue(response.getEntity().getContent());

            // Act
            File output = File.createTempFile("output", ".xml");
            output.deleteOnExit();
            new JUnitReportSerializer(output, result.executions).generate();

            // Assert
            assertTrue(fileEquals("report.xml", output));
        }
    }
}
