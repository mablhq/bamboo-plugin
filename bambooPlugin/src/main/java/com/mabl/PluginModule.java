package com.mabl;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClients;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class PluginModule extends AbstractModule {
    private static final String CONFIG_FILE = "config.properties";
    private static final String MAX_RETRIES = "com.mabl.http.retryer.max.retries";
    private static final String RETRY_INTERVAL = "com.mabl.http.retryer.retry.interval.milliseconds";
    private static final String REST_API_USERNAME_PLACEHOLDER = "key";
    private static final String PLUGIN_VERSION_UNKNOWN = "unknown";
    private static final String PLUGIN_USER_AGENT = "mabl-bamboo-plugin/" + PLUGIN_VERSION_UNKNOWN;
    private static final int REQUEST_TIMEOUT_MILLISECONDS = 60000;

    @Override
    protected void configure() {
        FileInputStream inputStream = null;
        try {
            Properties properties = new Properties();
            properties.load(getClass().getClassLoader().getResourceAsStream(CONFIG_FILE));
            Names.bindProperties(binder(), properties);
        } catch (IOException e) {
            System.out.println("ERROR: Could not load properties");
            throw new RuntimeException(e);
        } finally {
            try {
                if(inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

/*
    @Provides
    MablRestApiClientRetryHandler providesMablRestApiClientRetryHandler(
            @Named(MAX_RETRIES) int maxRetires,
            @Named(RETRY_INTERVAL) long retryInterval
    ) {
        return new MablRestApiClientRetryHandler(maxRetires, retryInterval);
    }


    @Provides
    CredentialsProvider getApiCredentialProvider(@Named("com.mabl.restApiKey") String restApiKey) {
        final CredentialsProvider provider = new BasicCredentialsProvider();
        final UsernamePasswordCredentials creds =
                new UsernamePasswordCredentials(REST_API_USERNAME_PLACEHOLDER, restApiKey);

        provider.setCredentials(AuthScope.ANY, creds);

        return provider;
    }

    @Provides
    RequestConfig providesRequestConfig() {
        return RequestConfig.custom()
                .setConnectTimeout(REQUEST_TIMEOUT_MILLISECONDS)
                .setConnectionRequestTimeout(REQUEST_TIMEOUT_MILLISECONDS)
                .setSocketTimeout(REQUEST_TIMEOUT_MILLISECONDS)
                .build();
    }

    @Provides
    CloseableHttpClient providesCloseableHttpClient(
            MablRestApiClientRetryHandler retryHandler,
            CredentialsProvider credentialsProvider,
            RequestConfig requestConfig
    ) {
        return HttpClients.custom()
                .setRedirectStrategy(new DefaultRedirectStrategy())
                .setServiceUnavailableRetryStrategy(retryHandler)
                .setDefaultCredentialsProvider(credentialsProvider)
                .setUserAgent(PLUGIN_USER_AGENT) // track calls @ API level
                .setConnectionTimeToLive(30, TimeUnit.SECONDS) // use keep alive in SSL API connections
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
*/
}
