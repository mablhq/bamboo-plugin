package com.mabl;

import com.mabl.domain.CreateDeploymentResult;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class RestApiClientTest extends AbstractWiremockTest {

    private static final String EXPECTED_DEPLOYMENT_EVENT_ID = "d1To4-GYeZ4nl-4Ag1JyQg-v";
    private static final String EXPECTED_ORGANIZATION_ID = "K8NWhtPqOyFnyvJTvCP0uw-w";

    @Test
    public void createDeploymentAllParametersHappyPathTest() throws IOException {

        final String fakeRestApiKey = "pa$$\\/\\/orD";
        final String environmentId = "foo-env-e";
        final String applicationId = "foo-app-a";

        registerPostMapping(
                RestApiClient.DEPLOYMENT_TRIGGER_ENDPOINT,
                MablTestConstants.CREATE_DEPLOYMENT_EVENT_RESULT_JSON,
                RestApiClient.REST_API_USERNAME_PLACEHOLDER,
                fakeRestApiKey,
                "{\"environment_id\":\"foo-env-e\",\"application_id\":\"foo-app-a\"}"
        );

        assertSuccessfulCreateDeploymentRequest(fakeRestApiKey, environmentId, applicationId);
    }

    private void assertSuccessfulCreateDeploymentRequest(
            final String restApiKey,
            final String environmentId,
            final String applicationId
    ) throws IOException {

        RestApiClient client = null;
        try {
            client = new RestApiClient();
            CreateDeploymentResult result = client.createDeploymentEvent(restApiKey, environmentId, applicationId);
            assertEquals(EXPECTED_DEPLOYMENT_EVENT_ID, result.id);
        } finally {
            if (client != null) {
                client.close();
            }
        }

        verifyExpectedUrls();
    }

}
