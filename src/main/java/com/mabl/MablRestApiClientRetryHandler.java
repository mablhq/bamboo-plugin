package com.mabl;

import org.apache.http.HttpResponse;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.protocol.HttpContext;

import java.util.ArrayList;
import java.util.Arrays;

import static org.apache.commons.httpclient.HttpStatus.SC_BAD_GATEWAY;
import static org.apache.commons.httpclient.HttpStatus.SC_NOT_IMPLEMENTED;
import static org.apache.commons.httpclient.HttpStatus.SC_SERVICE_UNAVAILABLE;

public class MablRestApiClientRetryHandler implements ServiceUnavailableRetryStrategy {
    private static final ArrayList<Integer> retryStatusCodes = new ArrayList<>(Arrays.asList(
            SC_NOT_IMPLEMENTED,
            SC_BAD_GATEWAY,
            SC_SERVICE_UNAVAILABLE
    ));
    private final int maxRetries;
    private final long retryIntervalMillis;

    public MablRestApiClientRetryHandler(final int maxRetries, final long retryIntervalMillis) {
        this.maxRetries = maxRetries;
        this.retryIntervalMillis = retryIntervalMillis;
    }

    @Override
    public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
        return isRetryStatusCode(response.getStatusLine().getStatusCode())
                && executionCount <= this.maxRetries;
    }

    @Override
    public long getRetryInterval() {
        return this.retryIntervalMillis;
    }

    private boolean isRetryStatusCode(int statusCode) {
        return retryStatusCodes.contains(statusCode);
    }

}
