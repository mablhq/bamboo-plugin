package com.mabl;

import com.atlassian.extras.common.log.Logger;
import com.mabl.domain.CreateDeploymentProperties;
import com.mabl.domain.CreateDeploymentResult;
import com.mabl.domain.CreateDeploymentPayload;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.mabl.domain.ExecutionResult;
import com.mabl.domain.GetApiKeyResult;
import com.mabl.domain.GetApplicationsResult;
import com.mabl.domain.GetEnvironmentsResult;
import com.mabl.domain.GetLabelsResult;

import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;

import static com.mabl.MablConstants.CONNECTION_TIMEOUT;
import static com.mabl.MablConstants.REQUEST_TIMEOUT;
import static com.mabl.Utils.getObjectMapperSingleton;
import static org.apache.commons.httpclient.HttpStatus.SC_CREATED;
import static org.apache.commons.httpclient.HttpStatus.SC_OK;

public class RestApiClient implements AutoCloseable {
    private final String restApiBaseUrl;
    private final String restApiKey;
    private final ProxyConfiguration proxyConfiguration;
    private final HttpClientContext httpClientContext;
    private final CloseableHttpClient httpClient;
    private final Logger.Log log = Logger.getInstance(this.getClass());

    static final String REST_API_USERNAME_PLACEHOLDER = "key";
    static final Header JSON_TYPE_HEADER = new BasicHeader("Content-Type", "application/json");
    static final String DEPLOYMENT_TRIGGER_ENDPOINT = "/events/deployment";
    static final String DEPLOYMENT_RESULT_ENDPOINT_TEMPLATE = "/execution/result/event/%s";
    static final String GET_API_KEY_ENDPOINT = "/apiKeys/self";
    static final String GET_APPLICATIONS_ENDPOINT_TEMPLATE = "/applications?organization_id=%s";
    static final String GET_ENVIRONMENTS_ENDPOINT_TEMPLATE = "/environments?organization_id=%s";
    static final String GET_LABELS_ENDPOINT_TEMPLATE = "/schedule/runPolicy/labels?organization_id=%s";

    public RestApiClient(String restApiBaseUrl, String restApiKey, ProxyConfiguration proxyConfiguration) {
        this.restApiBaseUrl = restApiBaseUrl;
        final HttpHost apiHost = HttpHost.create(restApiBaseUrl);
        this.restApiKey = restApiKey;
        this.proxyConfiguration = proxyConfiguration;
        final CredentialsProvider credentialsProvider = getCredentialsProvider(restApiKey, apiHost, proxyConfiguration);
        final RequestConfig requestConfig = requestConfig(proxyConfiguration);
        this.httpClientContext = httpClientContext(apiHost, credentialsProvider);
        this.httpClient = getHttpClient(credentialsProvider, requestConfig);
    }

    public String getRestApiKey() {
        return this.restApiKey;
    }

    public CreateDeploymentResult createDeploymentEvent(
            final String environmentId,
            final String applicationId,
            final Set<String> planLabels,
            final String mablBranch,
            final CreateDeploymentProperties properties
    ) {
        final HttpPost request = new HttpPost(restApiBaseUrl + DEPLOYMENT_TRIGGER_ENDPOINT);
        request.setEntity(getCreateDeploymentPayloadEntity(environmentId, applicationId, planLabels, mablBranch, properties));
        request.addHeader(JSON_TYPE_HEADER);
        return parseApiResult(getResponse(request), CreateDeploymentResult.class);
    }

    public ExecutionResult getExecutionResults(final String eventId) {
        final String url = restApiBaseUrl + String.format(DEPLOYMENT_RESULT_ENDPOINT_TEMPLATE, eventId);
        return parseApiResult(getResponse(new HttpGet(url)), ExecutionResult.class);
    }

    public GetApiKeyResult getApiKeySelf() {
        final String url = restApiBaseUrl + GET_API_KEY_ENDPOINT;
        return parseApiResult(getResponse(new HttpGet(url)), GetApiKeyResult.class);
    }

    public GetApplicationsResult getApplicationsResult(String organizationId) {
        final String url = restApiBaseUrl + String.format(GET_APPLICATIONS_ENDPOINT_TEMPLATE, organizationId);
        return parseApiResult(getResponse(new HttpGet(url)), GetApplicationsResult.class);
    }

    public GetEnvironmentsResult getEnvironmentsResult(String organizationId) {
        final String url = restApiBaseUrl + String.format(GET_ENVIRONMENTS_ENDPOINT_TEMPLATE, organizationId);
        return parseApiResult(getResponse(new HttpGet(url)), GetEnvironmentsResult.class);
    }

    public GetLabelsResult getLabelsResult(String organizationId) {
        final String url = restApiBaseUrl + String.format(GET_LABELS_ENDPOINT_TEMPLATE, organizationId);
        return parseApiResult(getResponse(new HttpGet(url)), GetLabelsResult.class);
    }

    private CloseableHttpClient getHttpClient(CredentialsProvider credentialsProvider, RequestConfig requestConfig) {

        return HttpClients.custom()
                .useSystemProperties() // use JVM proxy settings passed in by Bamboo.
                .setProxy(proxyConfiguration.getProxy().orElse(null))
                .setRedirectStrategy(new DefaultRedirectStrategy())
                .setServiceUnavailableRetryStrategy(getRetryHandler())
                .setDefaultCredentialsProvider(credentialsProvider)
                .setUserAgent(MablConstants.PLUGIN_USER_AGENT)
                .setConnectionTimeToLive(MablConstants.CONNECTION_SECONDS_TO_LIVE.getSeconds(), TimeUnit.SECONDS)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    protected MablRestApiClientRetryHandler getRetryHandler() {
    	return getRetryHandler(
                MablConstants.RETRY_HANDLER_MAX_RETRIES,
                MablConstants.RETRY_HANDLER_RETRY_INTERVAL.toMillis()
        );
    }

    protected static MablRestApiClientRetryHandler getRetryHandler(int maxRetries, long retryInterval) {
        return new MablRestApiClientRetryHandler(maxRetries, retryInterval);
    }

    private static CredentialsProvider getCredentialsProvider(final String restApiKey, final HttpHost apiHost, final ProxyConfiguration proxyConfiguration) {
        final CredentialsProvider provider = new BasicCredentialsProvider();
        final UsernamePasswordCredentials apiCreds =
                new UsernamePasswordCredentials(REST_API_USERNAME_PLACEHOLDER, restApiKey);

        // Set API key credential
        provider.setCredentials(new AuthScope(apiHost), apiCreds);

        // Set proxy credentials if provided
        proxyConfiguration.getProxy().ifPresent(proxy ->
        	proxyConfiguration.getCredentials().ifPresent(credentials ->
        		provider.setCredentials(new AuthScope(proxy), credentials)
        	)
        );
        return provider;
    }

    private static RequestConfig requestConfig(final ProxyConfiguration proxyConfiguration) {
        return RequestConfig.custom()
                .setConnectTimeout(Math.toIntExact(CONNECTION_TIMEOUT.toMillis()))
                .setConnectionRequestTimeout(Math.toIntExact(CONNECTION_TIMEOUT.toMillis()))
                .setSocketTimeout(Math.toIntExact(REQUEST_TIMEOUT.toMillis()))
                .setProxy(proxyConfiguration.getProxy().orElse(null))
                .setProxyPreferredAuthSchemes(Collections.singletonList(AuthSchemes.BASIC))
                .setTargetPreferredAuthSchemes(Collections.singletonList(AuthSchemes.BASIC))
                .build();
    }

    private static HttpClientContext httpClientContext(final HttpHost apiHost, final CredentialsProvider credentialsProvider) {
        AuthCache authCache = new BasicAuthCache();
        authCache.put(apiHost, new BasicScheme());
        HttpClientContext context = HttpClientContext.create();
        context.setCredentialsProvider(credentialsProvider);
        context.setAuthCache(authCache);
        return context;
    }

    private AbstractHttpEntity getCreateDeploymentPayloadEntity(
            String environmentId,
            String applicationId,
            Set<String> planLabels,
            String mablBranch,
            CreateDeploymentProperties properties
    ) {
        try {
            final String jsonPayload = getObjectMapperSingleton().writeValueAsString(
                    new CreateDeploymentPayload(
                            environmentId, applicationId, planLabels.isEmpty() ? null : planLabels,
                            StringUtils.isEmpty(mablBranch) ? null : mablBranch, properties)
            );

            return new ByteArrayEntity(jsonPayload.getBytes(StandardCharsets.UTF_8));

        } catch (IOException e) {
            log.error(String.format("Unable to create payloadEntity. Reason: '%s'", e.getMessage()));
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private HttpResponse getResponse(HttpUriRequest request) {
        try {
            return httpClient.execute(request, httpClientContext);
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

        switch (statusCode) {
            case SC_OK: // fall through case
            case SC_CREATED:
                return mapObject(response, resultClass);
            default:
                final String message = String.format(
                        "Unexpected status returned from parse Api result during fetch %d%n: [%s]",
                        statusCode,
                        response.getStatusLine().getReasonPhrase()
                );
                log.error(message);
                throw new RuntimeException(message);
        }
    }

    private <ApiResult> ApiResult mapObject(final HttpResponse response, Class<ApiResult> resultClass) {
        try {
            return resultClass.cast(getObjectMapperSingleton().readerFor(resultClass).readValue(response.getEntity().getContent()));
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
