package com.mabl;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Optional;

import org.apache.http.HttpHost;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

public class ProxyConfiguration {

	private Optional<HttpHost> maybeProxy = Optional.empty();
	private Optional<Credentials> maybeCredentials = Optional.empty();
	
	public ProxyConfiguration(String proxyAddress, String proxyUsername, String proxyPassword) {
		if(isNotBlank(proxyAddress)) {
			HttpHost proxy = HttpHost.create(proxyAddress);
			this.maybeProxy = Optional.of(proxy);
			if(isNotBlank(proxyUsername)) {
				Credentials credentials = new UsernamePasswordCredentials(proxyUsername, proxyPassword);
				maybeCredentials = Optional.of(credentials);
			}
		}
	}
	
	public Optional<HttpHost> getProxy() {
		return maybeProxy;
	}
	
	public Optional<Credentials> getCredentials() {
		return maybeCredentials;
	}
}
