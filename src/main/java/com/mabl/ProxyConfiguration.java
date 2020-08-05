package com.mabl;

import java.util.Optional;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class ProxyConfiguration {

	private Optional<HttpHost> maybeProxy = Optional.empty();
	private Optional<CredentialsProvider> maybeCredentialsProvider = Optional.empty();
	
	public ProxyConfiguration(String proxyAddress, String proxyUsername, String proxyPassword) {
		if(isNotBlank(proxyAddress)) {
			HttpHost proxy = HttpHost.create(proxyAddress);
			this.maybeProxy = Optional.of(proxy);
			if(isNotBlank(proxyUsername)) {
				CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
				Credentials credentials = new UsernamePasswordCredentials(proxyUsername, proxyPassword);
				credentialsProvider.setCredentials(new AuthScope(proxy), credentials);
				this.maybeCredentialsProvider = Optional.of(credentialsProvider);
			}
		}
	}
	
	public Optional<HttpHost> getProxy() {
		return maybeProxy;
	}
	
	public Optional<CredentialsProvider> getCredentialsProvider() {
		return maybeCredentialsProvider;
	}
}
