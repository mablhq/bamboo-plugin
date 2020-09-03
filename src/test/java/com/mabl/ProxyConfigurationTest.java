package com.mabl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpHost;
import org.apache.http.auth.Credentials;
import org.junit.Test;

public class ProxyConfigurationTest {

	private static final String PROXY_ADDRESS = "https://someproxy:8888";
	private static final String PROXY_HOST = "someproxy";
	private static final String PROXY_USERNAME = "someUsername";
	private static final String PROXY_PASSWORD = "somePassword";
	
	@Test
	public void validateProxyConfigWithNothingProvided() {
		ProxyConfiguration proxyConfig = new ProxyConfiguration(null, null, null);
		assertFalse(proxyConfig.getProxy().isPresent());
		assertFalse(proxyConfig.getCredentials().isPresent());
	}

	@Test
	public void validateProxyConfigWithProxyAndNoCredentials() {
		ProxyConfiguration proxyConfig = new ProxyConfiguration(PROXY_ADDRESS, null, null);
		assertTrue(proxyConfig.getProxy().isPresent());
		HttpHost proxyHost = proxyConfig.getProxy().get();
		assertEquals(proxyHost.getHostName(), PROXY_HOST);
		assertEquals(proxyHost.getPort(), 8888);
		assertEquals(proxyHost.getSchemeName(), "https");
		assertFalse(proxyConfig.getCredentials().isPresent());
	}

	@Test
	public void validateProxyConfigWithProxyAndCredentials() {
		ProxyConfiguration proxyConfig = new ProxyConfiguration(PROXY_ADDRESS, PROXY_USERNAME, PROXY_PASSWORD);
		assertTrue(proxyConfig.getProxy().isPresent());
		HttpHost proxyHost = proxyConfig.getProxy().get();
		assertEquals(proxyHost.getHostName(), PROXY_HOST);
		assertEquals(proxyHost.getPort(), 8888);
		assertEquals(proxyHost.getSchemeName(), "https");
		assertTrue(proxyConfig.getCredentials().isPresent());
		Credentials credentials = proxyConfig.getCredentials().get();
		assertEquals(credentials.getUserPrincipal().getName(), PROXY_USERNAME);
		assertEquals(credentials.getPassword(), PROXY_PASSWORD);
	}

	@Test
	public void validateProxyConfigWithNoProxyAndCredentials() {
		ProxyConfiguration proxyConfig = new ProxyConfiguration(null, PROXY_USERNAME, PROXY_PASSWORD);
		assertFalse(proxyConfig.getProxy().isPresent());
		assertFalse(proxyConfig.getCredentials().isPresent());
	}

	@Test
	public void validateProxyConfigWithProxyAndUserNoPass() {
		ProxyConfiguration proxyConfig = new ProxyConfiguration(PROXY_ADDRESS, PROXY_USERNAME, null);
		assertTrue(proxyConfig.getProxy().isPresent());
		HttpHost proxyHost = proxyConfig.getProxy().get();
		assertEquals(proxyHost.getHostName(), PROXY_HOST);
		assertEquals(proxyHost.getPort(), 8888);
		assertEquals(proxyHost.getSchemeName(), "https");
		assertTrue(proxyConfig.getCredentials().isPresent());
		Credentials credentials = proxyConfig.getCredentials().get();
		assertEquals(credentials.getUserPrincipal().getName(), PROXY_USERNAME);
		assertEquals(credentials.getPassword(), null);
	}

	@Test
	public void validateProxyConfigWithProxyAndPasswordNoUser() {
		ProxyConfiguration proxyConfig = new ProxyConfiguration(PROXY_ADDRESS, null, PROXY_PASSWORD);
		assertTrue(proxyConfig.getProxy().isPresent());
		HttpHost proxyHost = proxyConfig.getProxy().get();
		assertEquals(proxyHost.getHostName(), PROXY_HOST);
		assertEquals(proxyHost.getPort(), 8888);
		assertEquals(proxyHost.getSchemeName(), "https");
		assertFalse(proxyConfig.getCredentials().isPresent());
	}

	@Test
	public void validateProxyConfigWithCredentialsButNoProxy() {
		ProxyConfiguration proxyConfig = new ProxyConfiguration(null, PROXY_USERNAME, PROXY_PASSWORD);
		assertFalse(proxyConfig.getProxy().isPresent());
		assertFalse(proxyConfig.getCredentials().isPresent());
	}
}
