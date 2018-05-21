package com.mabl;

import com.atlassian.extras.common.log.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mabl.domain.CreateDeploymentResult;
import com.mabl.domain.CreateDeploymentPayload;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpParams;

import static org.apache.commons.httpclient.HttpStatus.SC_CREATED;
import static org.apache.commons.httpclient.HttpStatus.SC_NOT_FOUND;
import static org.apache.commons.httpclient.HttpStatus.SC_OK;

public class RestApiClient {
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Logger.Log log = Logger.getInstance(this.getClass());

    private static final String DEPLOYMENT_TRIGGER_ENDPOINT = "/events/deployment";
    private static final String REST_API_USERNAME_PLACEHOLDER = "key";
    private static final String restApiBaseUrl = "https://api.mabl.com";
    private static final String restApiKey = "jMzlz6eUkPYX4S5wOhOn5w"; //TODO DEBUG REMOVE THIS
    private static final Header JSON_TYPE_HEADER = new BasicHeader("Content-Type", "application/json");
    private static final String PLUGIN_VERSION_UNKNOWN = "unknown";
    private static final String PLUGIN_USER_AGENT = "mabl-bamboo-plugin/" + PLUGIN_VERSION_UNKNOWN;
    private static final int REQUEST_TIMEOUT_MILLISECONDS = 60000;

    private void log(String message) { // he said in quotes
        throw new RuntimeException(message);
    }

    private String requestToString(HttpPost request) {
        String result = String.format("DEBUG 52 Request" +
                "url='%s'" +
                "method='%s'",
                request.getURI(),
                request.getMethod()
        );
        Header[] headers = request.getAllHeaders();
        for(Header header : headers) {
            result += header.getName()+"="+header.getValue();
        }
        HttpParams params = request.getParams();
        result += "params="+params.toString();
        return result;
    }

    public RestApiClient() {
        this.httpClient = getHttpClient();
    }

    public CreateDeploymentResult createDeploymentEvent(final String restApiKey, final String environmentId, final String applicationId) {
        final String url = restApiBaseUrl + DEPLOYMENT_TRIGGER_ENDPOINT;
        final HttpPost request = new HttpPost(url);

        try {
            final String jsonPayload = objectMapper.writeValueAsString(new CreateDeploymentPayload(environmentId, applicationId));
            final AbstractHttpEntity payloadEntity = new ByteArrayEntity(jsonPayload.getBytes("UTF-8"));
            request.setEntity(payloadEntity);
        } catch (IOException e) {
            log.error(String.format("Unable to create payloadEntity"));
            throw new RuntimeException(e.getMessage(), e);
        }

        request.addHeader(getBasicAuthHeader(restApiKey));
        request.addHeader(JSON_TYPE_HEADER);
        log(requestToString(request));
        HttpResponse response = getResponse(request);
        return parseApiResult(response, CreateDeploymentResult.class);
    }

    private CloseableHttpClient getHttpClient() {
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

}
