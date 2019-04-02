package com.mabl;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.atlassian.extras.common.log.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mabl.domain.CreateDeploymentProperties;
import com.mabl.domain.CreateDeploymentResult;
import com.mabl.domain.CreateDeploymentPayload;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.mabl.domain.ExecutionResult;
import com.mabl.domain.GetApiKeyResult;
import com.mabl.domain.GetApplicationsResult;
import com.mabl.domain.GetEnvironmentsResult;
import com.mabl.domain.GetLabelsResult;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.mabl.MablConstants.REQUEST_TIMEOUT_MILLISECONDS;
import static org.apache.commons.httpclient.HttpStatus.SC_CREATED;
import static org.apache.commons.httpclient.HttpStatus.SC_OK;

public class RestApiClient implements AutoCloseable {
    private final String restApiBaseUrl;
    private final String restApiKey;
    private final CloseableHttpClient httpClient;
    private final Logger.Log log = Logger.getInstance(this.getClass());

    private final ObjectMapper objectMapper = new ObjectMapper();
    {
        objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
    }

    static final String REST_API_USERNAME_PLACEHOLDER = "key";
    static final Header JSON_TYPE_HEADER = new BasicHeader("Content-Type", "application/json");
    static final String DEPLOYMENT_TRIGGER_ENDPOINT = "/events/deployment";
    static final String DEPLOYMENT_RESULT_ENDPOINT_TEMPLATE = "/execution/result/event/%s";
    static final String GET_API_KEY_ENDPOINT_TEMPLATE = "/apiKeys/%s";
    static final String GET_APPLICATIONS_ENDPOINT_TEMPLATE = "/applications?organization_id=%s";
    static final String GET_ENVIRONMENTS_ENDPOINT_TEMPLATE = "/environments?organization_id=%s";
    static final String GET_LABELS_ENDPOINT_TEMPLATE = "/schedule/runPolicy/labels?organization_id=%s";

    public RestApiClient(String restApiBaseUrl, String restApiKey) {
        this.restApiBaseUrl = restApiBaseUrl;
        this.restApiKey = restApiKey;
        this.httpClient = getHttpClient(restApiKey);
    }

    public String getRestApiKey() {
        return this.restApiKey;
    }

    public CreateDeploymentResult createDeploymentEvent(
            final String environmentId,
            final String applicationId,
            final Set<String> planLabels,
            final CreateDeploymentProperties properties
    ) {
        final HttpPost request = new HttpPost(restApiBaseUrl + DEPLOYMENT_TRIGGER_ENDPOINT);
        request.setEntity(getCreateDeploymentPayloadEntity(environmentId, applicationId, planLabels, properties));
        request.addHeader(getBasicAuthHeader(restApiKey));
        request.addHeader(JSON_TYPE_HEADER);
        return parseApiResult(getResponse(request), CreateDeploymentResult.class);
    }

    public ExecutionResult getExecutionResults(final String eventId) {
        final String url = restApiBaseUrl + String.format(DEPLOYMENT_RESULT_ENDPOINT_TEMPLATE, eventId);
        return parseApiResult(getResponse(buildGetRequest(url)), ExecutionResult.class);
    }

    public GetApiKeyResult getApiKeyResult(String formApiKey) {
        final String url = restApiBaseUrl + String.format(GET_API_KEY_ENDPOINT_TEMPLATE, formApiKey);
        return parseApiResult(getResponse(buildGetRequest(url)), GetApiKeyResult.class);
    }

    public GetApplicationsResult getApplicationsResult(String organizationId) {
        final String url = restApiBaseUrl + String.format(GET_APPLICATIONS_ENDPOINT_TEMPLATE, organizationId);
        return parseApiResult(getResponse(buildGetRequest(url)), GetApplicationsResult.class);
    }

    public GetEnvironmentsResult getEnvironmentsResult(String organizationId) {
        final String url = restApiBaseUrl + String.format(GET_ENVIRONMENTS_ENDPOINT_TEMPLATE, organizationId);
        return parseApiResult(getResponse(buildGetRequest(url)), GetEnvironmentsResult.class);
    }

    public GetLabelsResult getLabelsResult(String organizationId) {
        final String url = restApiBaseUrl + String.format(GET_LABELS_ENDPOINT_TEMPLATE, organizationId);
        return parseApiResult(getResponse(buildGetRequest(url)), GetLabelsResult.class);
    }

    private CloseableHttpClient getHttpClient(String restApiKey) {
        return HttpClients.custom()
                .useSystemProperties() // use JVM proxy settings passed in by Bamboo.
                .setRedirectStrategy(new DefaultRedirectStrategy())
                .setServiceUnavailableRetryStrategy(getRetryHandler())
                .setDefaultCredentialsProvider(getApiCredentialsProvider(restApiKey))
                .setUserAgent(MablConstants.PLUGIN_USER_AGENT)
                .setConnectionTimeToLive(MablConstants.CONNECTION_SECONDS_TO_LIVE, TimeUnit.SECONDS)
                .setDefaultRequestConfig(getDefaultRequestConfig())
                .build();
    }

    protected MablRestApiClientRetryHandler getRetryHandler() {
        return new MablRestApiClientRetryHandler(
                MablConstants.RETRY_HANDLER_MAX_RETRIES,
                MablConstants.RETRY_HANDLER_RETRY_INTERVAL
        );
    }

    private CredentialsProvider getApiCredentialsProvider(final String restApiKey) {
        final CredentialsProvider provider = new BasicCredentialsProvider();
        final UsernamePasswordCredentials creds =
                new UsernamePasswordCredentials(REST_API_USERNAME_PLACEHOLDER, restApiKey);

        provider.setCredentials(AuthScope.ANY, creds);
        return provider;
    }

    private RequestConfig getDefaultRequestConfig() {
        return RequestConfig.custom()
                .setConnectTimeout(REQUEST_TIMEOUT_MILLISECONDS)
                .setConnectionRequestTimeout(REQUEST_TIMEOUT_MILLISECONDS)
                .setSocketTimeout(REQUEST_TIMEOUT_MILLISECONDS)
                .build();
    }

    private AbstractHttpEntity getCreateDeploymentPayloadEntity(
            String environmentId,
            String applicationId,
            Set<String> planLabels,
            CreateDeploymentProperties properties
    ) {
        try {
            final String jsonPayload = objectMapper.writeValueAsString(
                    new CreateDeploymentPayload(environmentId, applicationId, planLabels.isEmpty() ? null : planLabels, properties)
            );

            return new ByteArrayEntity(jsonPayload.getBytes("UTF-8"));

        } catch (IOException e) {
            log.error(String.format("Unable to create payloadEntity. Reason: '%s'", e.getMessage()));
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private Header getBasicAuthHeader(final String restApiKey) {
        final String toEncode = REST_API_USERNAME_PLACEHOLDER + ":" + restApiKey;
        return new BasicHeader("Authorization", "Basic " + base64EncodeUtf8(toEncode));
    }

    private String base64EncodeUtf8(String toBeEncoded) {
        try {
            return Base64.getEncoder().encodeToString(toBeEncoded.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            log.error(String.format(
                    "Unable to base64 encode using UTF-8 the string '%s'. Reason:'%s'",
                    toBeEncoded,
                    e.getMessage()
            ));
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private HttpGet buildGetRequest(final String url) {
        final HttpGet request = new HttpGet(url);
        request.addHeader(getBasicAuthHeader(restApiKey));
        return request;
    }

    private HttpResponse getResponse(HttpUriRequest request) {
        try {
            return httpClient.execute(request);
        } catch (IOException e) {
            log.error(String.format(
                    "Encountered exception trying to reach '%s'. Reason: '%s'",
                    request.getURI(),
                    e.getMessage()
            ));
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private <ApiResult> ApiResult parseApiResult(final HttpResponse response, Class<ApiResult> resultClass) {
        final int statusCode = response.getStatusLine().getStatusCode();

        String message = "";
        switch (statusCode) {
            case SC_OK: // fall through case
            case SC_CREATED:
                return mapObject(response, resultClass);
            default:
                message = String.format(
                        "Unexpected status returned from parse Api result during fetch %d%n",
                        statusCode
                );
                log.error(message);
                throw new RuntimeException(message);
        }
    }

    private <ApiResult> ApiResult mapObject(final HttpResponse response, Class<ApiResult> resultClass) {
        try {
            return resultClass.cast(objectMapper.readerFor(resultClass).readValue(response.getEntity().getContent()));
        } catch (IOException e) {
            log.error(String.format(
                    "Encountered exception trying to decode '%s'. Reason: '%s'",
                    resultClass.getName(),
                    e.getMessage()
            ));
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void close() {
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException e) {
                log.error(String.format(
                        "Encountered exception trying to close httpClient. Reason: '%s'",
                        e.getMessage()
                ));
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

}
