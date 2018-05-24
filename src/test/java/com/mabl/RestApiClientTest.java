package com.mabl;

import com.mabl.domain.CreateDeploymentResult;
import com.mabl.domain.ExecutionResult;
import com.mabl.domain.GetApiKeyResult;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static com.mabl.RestApiClient.REST_API_USERNAME_PLACEHOLDER;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;

public class RestApiClientTest extends AbstractWiremockTest {
    private static final String fakeRestApiKey = "fakeApiKey";
    private static final String fakeEnvironmentId = "fakeEnvironmentId-e";
    private static final String fakeApplicationId = "fakeApplicationId-a";
    private static final String fakeEventId = "fakeEventId";

    @Test
    public void getApiKeyResultTest() {
        RestApiClient restApiClient = new RestApiClient(getBaseUrl(), fakeRestApiKey);

        registerGetMapping(
                String.format(restApiClient.GET_API_KEY_ENDPOINT_TEMPLATE, fakeRestApiKey),
                ok(),
                MablTestConstants.APIKEY_RESULT_JSON,
                REST_API_USERNAME_PLACEHOLDER,
                fakeRestApiKey
        );

        GetApiKeyResult getApiKeyResult = restApiClient.getApiKeyResult(fakeRestApiKey);
        assertEquals(MablTestConstants.ORGANIZATIONID_RESULT, getApiKeyResult.organization_id);
        verifyExpectedUrls();
    }

    @Test
    public void createDeploymentAllParametersHappyPathTest() {
        final String expectedBody = "{\"environment_id\":\""+fakeEnvironmentId+"\",\"application_id\":\""+fakeApplicationId+"\"}";

        registerPostMapping(
                RestApiClient.DEPLOYMENT_TRIGGER_ENDPOINT,
                MablTestConstants.CREATE_DEPLOYMENT_EVENT_RESULT_JSON,
                REST_API_USERNAME_PLACEHOLDER,
                fakeRestApiKey,
                expectedBody
        );

        assertSuccessfulCreateDeploymentRequest(fakeEnvironmentId, fakeApplicationId);
    }

    @Test
    public void createDeploymentOnlyEnvironmentIdHappyPathTest() {
        final String expectedBody = "{\"environment_id\":\""+fakeEnvironmentId+"\"}";
        final String nullApplicationId = null;

        registerPostMapping(
                RestApiClient.DEPLOYMENT_TRIGGER_ENDPOINT,
                MablTestConstants.CREATE_DEPLOYMENT_EVENT_RESULT_JSON,
                REST_API_USERNAME_PLACEHOLDER,
                fakeRestApiKey,
                expectedBody
        );

        assertSuccessfulCreateDeploymentRequest(fakeEnvironmentId, nullApplicationId);
    }

    @Test
    public void createDeploymentOnlyApplicationIdHappyPathTest() {
        final String expectedBody = "{\"application_id\":\""+fakeApplicationId+"\"}";
        final String nullEnvironmentId = null;

        registerPostMapping(
                RestApiClient.DEPLOYMENT_TRIGGER_ENDPOINT,
                MablTestConstants.CREATE_DEPLOYMENT_EVENT_RESULT_JSON,
                REST_API_USERNAME_PLACEHOLDER,
                fakeRestApiKey,
                expectedBody
        );

        assertSuccessfulCreateDeploymentRequest(nullEnvironmentId, fakeApplicationId);
    }

    private void assertSuccessfulCreateDeploymentRequest(final String environmentId, final String applicationId) {
        RestApiClient client = new RestApiClient(getBaseUrl(), fakeRestApiKey);
        CreateDeploymentResult result = client.createDeploymentEvent(environmentId, applicationId);
        assertEquals(MablTestConstants.EXPECTED_DEPLOYMENT_EVENT_ID, result.id);

        verifyExpectedUrls();
    }

    @Test
    public void getExecutionResultHappyPathTest() {
        registerGetMapping(
                String.format(RestApiClient.DEPLOYMENT_RESULT_ENDPOINT_TEMPLATE, fakeEventId),
                ok(),
                MablTestConstants.EXECUTION_RESULT_JSON,
                REST_API_USERNAME_PLACEHOLDER,
                fakeRestApiKey
        );

        RestApiClient client = new RestApiClient(getBaseUrl(), fakeRestApiKey);
            ExecutionResult result = client.getExecutionResults(fakeEventId);
            assertEquals("succeeded", result.executions.get(0).status);
            assertTrue("expected success", result.executions.get(0).success);

        verifyExpectedUrls();
    }

    @Test(expected = RuntimeException.class)
    public void getExecutionResultNotFoundTest() {

        registerGetMapping(
                String.format(RestApiClient.DEPLOYMENT_RESULT_ENDPOINT_TEMPLATE, fakeEventId),
                notFound(),
                MablTestConstants.EXECUTION_RESULT_JSON,
                REST_API_USERNAME_PLACEHOLDER,
                fakeRestApiKey
        );
        RestApiClient client = new RestApiClient(getBaseUrl(), fakeRestApiKey);
        client.getExecutionResults(fakeEventId);
        verifyExpectedUrls();
    }

}
