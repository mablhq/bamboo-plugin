package com.mabl;
//
//import com.github.tomakehurst.wiremock.client.MappingBuilder;
//import com.mabl.domain.CreateDeploymentResult;
//import com.github.tomakehurst.wiremock.matching.EqualToPattern;
//import org.junit.Test;
//
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.Map;
//
//import static org.junit.Assert.assertEquals;
//import static com.github.tomakehurst.wiremock.client.WireMock.post;
//import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
//import static com.github.tomakehurst.wiremock.client.WireMock.created;
//import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
//
public class RestApiClientTest extends AbstractWiremockTest {
//
//    private static final String EXPECTED_DEPLOYMENT_EVENT_ID = "d1To4-GYeZ4nl-4Ag1JyQg-v";
//    private static final String EXPECTED_ORGANIZATION_ID = "K8NWhtPqOyFnyvJTvCP0uw-w";
//    private Map<String, String> expectedUrls = new HashMap<>();
//
//    @Test
//    public void createDeploymentAllParametersHappyPathTest() throws IOException {
//
//        //final String fakeRestApiKey = "jMzlz6eUkPYX4S5wOhOn5w";
//        //final String environmentId = "foo-env-e";
//        //final String applicationId = "foo-app-a";
//
//        //final String expectedBody = "{\"environment_id\":\"foo-env-e\",\"application_id\":\"foo-app-a\"}";
//
//        //final String mappedUrl = generatePageUrl(RestApiClient.DEPLOYMENT_TRIGGER_ENDPOINT);
//        //expectedUrls.put(RestApiClient.DEPLOYMENT_TRIGGER_ENDPOINT, "POST");
//
//        //final MappingBuilder mappingBuilder = post(urlPathEqualTo(RestApiClient.DEPLOYMENT_TRIGGER_ENDPOINT))
//        //        .willReturn(created()
//        //                .withHeader("Content-Type", "application/json")
//        //                .withBody(MablTestConstants.CREATE_DEPLOYMENT_EVENT_RESULT_JSON));
//
//        //mappingBuilder.withBasicAuth(RestApiClient.REST_API_USERNAME_PLACEHOLDER, fakeRestApiKey);
//        //mappingBuilder.withHeader("user-agent", new EqualToPattern(PLUGIN_USER_AGENT));
//        //mappingBuilder.withHeader("Content-Type", new EqualToPattern("application/json"));
//        //mappingBuilder.withRequestBody(new EqualToPattern(expectedBody));
//
//        //stubFor(mappingBuilder);
//
//        //assertSuccessfulCreateDeploymentRequest(fakeRestApiKey, environmentId, applicationId);
//    }
//
//    private String generatePageUrl(final String path) {
//        return getBaseUrl() + path;
//    }
//
//    protected String getBaseUrl() {
//        final int portNumber = wireMockRule.getOptions().portNumber();
//        final String address = wireMockRule.getOptions().bindAddress();
//        return String.format("http://%s:%d", address, portNumber);
//    }
//
//    private void assertSuccessfulCreateDeploymentRequest(
//            final String restApiKey,
//            final String environmentId,
//            final String applicationId
//    ) throws IOException {
//
//        RestApiClient client = null;
//        try {
//            client = new RestApiClient();
//            CreateDeploymentResult result = client.createDeploymentEvent(restApiKey, environmentId, applicationId);
//            assertEquals(EXPECTED_DEPLOYMENT_EVENT_ID, result.id);
//        } finally {
//            if (client != null) {
//                client.close();
//            }
//        }
//
//        verifyExpectedUrls();
//    }

}
