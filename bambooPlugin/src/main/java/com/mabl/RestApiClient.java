package com.mabl;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.atlassian.extras.common.log.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mabl.domain.CreateDeploymentResult;
import com.mabl.domain.CreateDeploymentPayload;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import com.mabl.domain.ExecutionResult;
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
import static org.apache.commons.httpclient.HttpStatus.SC_CREATED;
import static org.apache.commons.httpclient.HttpStatus.SC_NOT_FOUND;
import static org.apache.commons.httpclient.HttpStatus.SC_OK;

public class RestApiClient {
    private final String restApiKey;
    private final CloseableHttpClient httpClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    {
        objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    }
    private final Logger.Log log = Logger.getInstance(this.getClass());

    static final String DEPLOYMENT_TRIGGER_ENDPOINT = "/events/deployment";
    static final String DEPLOYMENT_RESULT_ENDPOINT_TEMPLATE = "/execution/result/event/%s";
    static final String REST_API_USERNAME_PLACEHOLDER = "key";
    private static final String restApiBaseUrl = "https://api.mabl.com";
    private static final Header JSON_TYPE_HEADER = new BasicHeader("Content-Type", "application/json");
    private static final String PLUGIN_VERSION_UNKNOWN = "unknown";
    private static final String PLUGIN_USER_AGENT = "mabl-bamboo-plugin/" + PLUGIN_VERSION_UNKNOWN;
    private static final int REQUEST_TIMEOUT_MILLISECONDS = 60000;

    public RestApiClient(String restApiKey) {
        this.restApiKey = restApiKey;
        this.httpClient = getHttpClient(restApiKey);
    }

    public CreateDeploymentResult createDeploymentEvent(final String environmentId, final String applicationId) {
        final HttpPost request = buildPostRequest(restApiBaseUrl + DEPLOYMENT_TRIGGER_ENDPOINT);
        request.setEntity(getCreateDeplotmentPayloadEntity(environmentId, applicationId));
        request.addHeader(getBasicAuthHeader(restApiKey));
        request.addHeader(JSON_TYPE_HEADER);
        return parseApiResult(getResponse(request), CreateDeploymentResult.class);
    }

    public ExecutionResult getExecutionResults(final String eventId) {
        final String url = restApiBaseUrl + String.format(DEPLOYMENT_RESULT_ENDPOINT_TEMPLATE, eventId);
        return parseApiResult(getResponse(buildGetRequest(url)), ExecutionResult.class);
    }

    private HttpGet buildGetRequest(final String url) {
        try {
            final HttpGet request = new HttpGet(url);
            request.addHeader(getBasicAuthHeader(restApiKey));
            return request;
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(String.format("Unexpected status from mabl trying to build API url: %s", url));
        }
    }

    private AbstractHttpEntity getCreateDeplotmentPayloadEntity(String environmentId, String applicationId) {
        try {
            final String jsonPayload = objectMapper.writeValueAsString(new CreateDeploymentPayload(environmentId, applicationId));
            return new ByteArrayEntity(jsonPayload.getBytes("UTF-8"));

        } catch (IOException e) {
            log.error(String.format("Unable to create payloadEntity"));
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private CloseableHttpClient getHttpClient(String restApiKey) {
        return HttpClients.custom()
                .setRedirectStrategy(new DefaultRedirectStrategy())
                .setServiceUnavailableRetryStrategy(getRetryHandler())
                .setDefaultCredentialsProvider(getApiCredentialsProvider(restApiKey))
                .setUserAgent(PLUGIN_USER_AGENT) // track calls @ API level
                .setConnectionTimeToLive(30, TimeUnit.SECONDS) // use keep alive in SSL API connections
                .setDefaultRequestConfig(getDefaultRequestConfig())
                .build();
    }

    private RequestConfig getDefaultRequestConfig() {
        // TODO we should retry connection timeouts
        return RequestConfig.custom()
                .setConnectTimeout(REQUEST_TIMEOUT_MILLISECONDS)
                .setConnectionRequestTimeout(REQUEST_TIMEOUT_MILLISECONDS)
                .setSocketTimeout(REQUEST_TIMEOUT_MILLISECONDS)
                .build();
    }
    private MablRestApiClientRetryHandler getRetryHandler() {
        return new MablRestApiClientRetryHandler(5, 6000L);
    }

    private CredentialsProvider getApiCredentialsProvider(
            final String restApiKey
    ) {
        final CredentialsProvider provider = new BasicCredentialsProvider();
        final UsernamePasswordCredentials creds =
                new UsernamePasswordCredentials(REST_API_USERNAME_PLACEHOLDER, restApiKey);

        provider.setCredentials(AuthScope.ANY, creds);

        return provider;
    }

    private Header getBasicAuthHeader(final String restApiKey) {
        final String toEncode = REST_API_USERNAME_PLACEHOLDER + ":" + restApiKey;
        return new BasicHeader("Authorization", "Basic " + base64EncodeUtf8(toEncode));
    }

    private String base64EncodeUtf8(String s) {
        try {
            return Base64.getEncoder().encodeToString(s.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            log.error(String.format("Unable to base64 encode using UTF-8 the string '%s'", s));
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private HttpPost buildPostRequest(String url) {
        try {
            final HttpPost request = new HttpPost(url);
            return request;
        } catch (IllegalArgumentException e) {
            log.error(String.format("Unexpected status from mabl trying to build API url: %s", url));
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private HttpResponse getResponse(HttpUriRequest request) {
        try {
            return httpClient.execute(request);
        } catch (IOException e) {
            log.error(String.format("Encountered exception trying to reach '%s' Reason: %s", request.getURI(), e.getMessage()));
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private <ApiResult> ApiResult parseApiResult(final HttpResponse response, Class<ApiResult> resultClass) {
        final int statusCode = response.getStatusLine().getStatusCode();

        switch (statusCode) {
            case SC_OK: // fall through case
            case SC_CREATED:
                return mapObject(response, resultClass);
            case SC_NOT_FOUND:
                return null;
            default:
                String message = String.format("Unexpected status from parse Api result during fetch %d%n", statusCode);
                log.error(message);
                throw new RuntimeException(message);
        }
    }

    private <ApiResult> ApiResult mapObject(final HttpResponse response, Class<ApiResult> resultClass) {
        try {
            return resultClass.cast(objectMapper.readerFor(resultClass).readValue(response.getEntity().getContent()));
        } catch (IOException e) {
            log.error(String.format("Encountered exception trying to decode '%s' Reason: %s", resultClass.getName(), e.getMessage()));
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void close() {
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException e) {
                // TODO cleaner exception handling
                e.printStackTrace();
            }
        }
    }
}
