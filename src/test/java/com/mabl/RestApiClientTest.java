package com.mabl;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.mabl.domain.CreateDeploymentProperties;
import com.mabl.domain.CreateDeploymentResult;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.mabl.domain.ExecutionResult;
import com.mabl.domain.GetApiKeyResult;
import com.mabl.domain.GetApplicationsResult;
import com.mabl.domain.GetEnvironmentsResult;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static com.mabl.RestApiClient.REST_API_USERNAME_PLACEHOLDER;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static org.apache.commons.httpclient.HttpStatus.SC_CREATED;

public class RestApiClientTest extends AbstractWiremockTest {
    private static final String fakeRestApiKey = "fakeApiKey";
    private static final String fakeEnvironmentId = "xolMgRp4CwvHQjQUX_MOoA-e";
    private static final String fakeApplicationId = "smoTxTR8B9oh73qstERNyg-a";
    private static final String fakeEventId = "fakeEventId";
    private static final String fakePlanLabels = "[\"labelA\",\"labelB\"]";
    private static final Set<String> expectedPlanLabels = new HashSet<>(Arrays.asList("labelA","labelB"));
    private static final Set<String> emptyPlanLabels = new HashSet<>();
    private static final String fakeProperties = "{\"deployment_origin\":\""+MablConstants.PLUGIN_USER_AGENT+"\"}";

    class PartialRestApiClient extends RestApiClient {

        public PartialRestApiClient(String restApiBaseUrl, String restApiKey) {
            super(restApiBaseUrl, restApiKey, new ProxyConfiguration(null, null, null));
        }

        @Override
        protected MablRestApiClientRetryHandler getRetryHandler() {
            return new MablRestApiClientRetryHandler(5, 0);
        }
    }

    @Test
    public void getApiKeyResultTest() {
        RestApiClient restApiClient = new PartialRestApiClient(getBaseUrl(), fakeRestApiKey);

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
        final String expectedBody = "{\"environment_id\":\""+fakeEnvironmentId+"\",\"application_id\":\""+fakeApplicationId+"\",\"plan_labels\":"+fakePlanLabels+",\"properties\":"+fakeProperties+"}";

        registerPostMapping(
                RestApiClient.DEPLOYMENT_TRIGGER_ENDPOINT,
                MablTestConstants.buildDeploymentResultJson(fakeEnvironmentId, fakeApplicationId, fakePlanLabels),
                REST_API_USERNAME_PLACEHOLDER,
                fakeRestApiKey,
                expectedBody
        );

        assertSuccessfulCreateDeploymentRequest(fakeEnvironmentId, fakeApplicationId, expectedPlanLabels);
    }

    @Test
    public void createDeploymentOnlyEnvironmentIdHappyPathTest() {
        final String expectedBody = "{\"environment_id\":\""+fakeEnvironmentId+"\",\"properties\":"+fakeProperties+"}";

        registerPostMapping(
                RestApiClient.DEPLOYMENT_TRIGGER_ENDPOINT,
                MablTestConstants.buildDeploymentResultJson(fakeEnvironmentId, null, null),
                REST_API_USERNAME_PLACEHOLDER,
                fakeRestApiKey,
                expectedBody
        );

        assertSuccessfulCreateDeploymentRequest(fakeEnvironmentId, null, emptyPlanLabels);
    }

    @Test
    public void createDeploymentOnlyApplicationIdHappyPathTest() {
        final String expectedBody = "{\"application_id\":\""+fakeApplicationId+"\",\"properties\":"+fakeProperties+"}";

        registerPostMapping(
                RestApiClient.DEPLOYMENT_TRIGGER_ENDPOINT,
                MablTestConstants.buildDeploymentResultJson(null, fakeApplicationId, null),
                REST_API_USERNAME_PLACEHOLDER,
                fakeRestApiKey,
                expectedBody
        );

        assertSuccessfulCreateDeploymentRequest(null, fakeApplicationId, emptyPlanLabels);
    }

    private void assertSuccessfulCreateDeploymentRequest(final String environmentId, final String applicationId, final Set<String> planLabels) {
        RestApiClient client = new PartialRestApiClient(getBaseUrl(), fakeRestApiKey);
        CreateDeploymentProperties properties = new CreateDeploymentProperties();
        properties.setDeploymentOrigin(MablConstants.PLUGIN_USER_AGENT);
        CreateDeploymentResult result = client.createDeploymentEvent(environmentId, applicationId, planLabels, properties);

        assertEquals(MablTestConstants.EXPECTED_DEPLOYMENT_EVENT_ID, result.id);
        if(environmentId == null) {
            assertNull(result.environmentId);
        } else {
            assertEquals(MablTestConstants.EXPECTED_DEPLOYMENT_EVENT_ENVIRONMENT_ID, result.environmentId);
        }

        if(applicationId == null) {
            assertNull(result.applicationId);
        } else {
            assertEquals(MablTestConstants.EXPECTED_DEPLOYMENT_EVENT_APPLICATION_ID, result.applicationId);
        }

        if(planLabels.isEmpty()) {
            assertNull(result.planLabels);
        } else {
            assertEquals(planLabels, result.planLabels);
        }

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

        RestApiClient client = new PartialRestApiClient(getBaseUrl(), fakeRestApiKey);
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
        RestApiClient client = new PartialRestApiClient(getBaseUrl(), fakeRestApiKey);
        client.getExecutionResults(fakeEventId);
        verifyExpectedUrls();
    }

    @Test
    public void getApplicationsReturnsTwoResults() {

        final String fakeRestApiKey = "fakeApiKeyValue";
        final String organization_id = "fakeOrganizationId";

        WireMock.stubFor(get(urlPathEqualTo("/applications"))
                .withQueryParam("organization_id", equalTo(organization_id))
                .withBasicAuth(REST_API_USERNAME_PLACEHOLDER, fakeRestApiKey)
                .withHeader("user-agent", new EqualToPattern(MablConstants.PLUGIN_USER_AGENT))
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody(MablTestConstants.APPLICATIONS_RESULT_JSON)
                ));

        final String baseUrl = getBaseUrl();

        RestApiClient client = new PartialRestApiClient(baseUrl, fakeRestApiKey);
        GetApplicationsResult result = client.getApplicationsResult(organization_id);
        assertEquals(2, result.applications.size());
    }

    @Test
    public void getEnvironmentsReturnsOneResult() {

        final String fakeRestApiKey = "fakeApiKeyValue";
        final String organization_id = "fakeOrganizationId";

        WireMock.stubFor(get(urlPathEqualTo("/environments"))
                .withQueryParam("organization_id", equalTo(organization_id))
                .withBasicAuth(REST_API_USERNAME_PLACEHOLDER, fakeRestApiKey)
                .withHeader("user-agent", new EqualToPattern(MablConstants.PLUGIN_USER_AGENT))
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody(MablTestConstants.ENVIRONMENTS_RESULT_JSON)
                ));
        final String baseUrl = getBaseUrl();

        RestApiClient client = new PartialRestApiClient(baseUrl, fakeRestApiKey);
        GetEnvironmentsResult result = client.getEnvironmentsResult(organization_id);
        assertEquals(1, result.environments.size());
    }

    @Test(expected = RuntimeException.class)
    public void apiClientDoesntRetryOn500() {
        registerPostCreateRetryMappings("/events/deployment", "500", 500, 1);
        assertSuccessfulCreateDeploymentRequest(fakeEnvironmentId, fakeApplicationId, emptyPlanLabels);
    }

    @Test
    public void apiClientRetriesOn501() {
        registerPostCreateRetryMappings("/events/deployment", "501", 501, 1);
        assertSuccessfulCreateDeploymentRequest(fakeEnvironmentId, fakeApplicationId, emptyPlanLabels);
    }

    @Test
    public void apiClientDoesRetryOn503() {
        registerPostCreateRetryMappings("/events/deployment", "503", 503, 1);
        assertSuccessfulCreateDeploymentRequest(fakeEnvironmentId, fakeApplicationId, emptyPlanLabels);
    }

    @Test
    public void apiClientRetriesOn501MaxtimesSuccess() {
        registerPostCreateRetryMappings("/events/deployment", "501", 501, 5);
        assertSuccessfulCreateDeploymentRequest(fakeEnvironmentId, fakeApplicationId, emptyPlanLabels);
    }

    @Test(expected = RuntimeException.class)
    public void apiClientRetriesOn501OverMaxtimesFailure() {
        registerPostCreateRetryMappings("/events/deployment", "501", 501, 6);
        assertSuccessfulCreateDeploymentRequest(fakeEnvironmentId, fakeApplicationId, emptyPlanLabels);
    }

    private void registerPostCreateRetryMappings(
            final String postUrl,
            final String scenario,
            final int status,
            final int numTimes
    ) {
        String whenState = Scenario.STARTED;
        for(int i=1;i<=numTimes;i++) {
            String willState = "Requested "+i+" Times";
            stubFor(post(urlEqualTo(postUrl))
                    .inScenario(scenario)
                    .whenScenarioStateIs(whenState)
                    .willSetStateTo(willState)
                    .willReturn(aResponse()
                            .withStatus(status)
                            .withHeader("Content-Type", "application/json")
                            .withBody(""+status))
            );

            whenState = willState;
        }

        stubFor(post(urlEqualTo(postUrl))
                .inScenario(scenario)
                .whenScenarioStateIs(whenState)
                .willReturn(aResponse()
                        .withStatus(SC_CREATED)
                        .withHeader("Content-Type", "application/json")
                        .withBody(MablTestConstants.buildDeploymentResultJson(fakeEnvironmentId, fakeApplicationId, null)))
        );
    }
}
