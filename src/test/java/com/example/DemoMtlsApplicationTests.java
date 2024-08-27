package com.example;

import java.net.http.HttpClient;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManagerFactory;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.profiles.active=tls,mtls")
class DemoMtlsApplicationTests {
	@LocalServerPort int port;

	@Autowired RestClient.Builder restClientBuilder;

	@Test
	void healthCheckWithValidCertificate() {
		KeyStore keyStore = CertUtils.createKeyStore("classpath:self-signed/client.key", "classpath:self-signed/client.crt", "dummy");
		KeyStore trustStore = CertUtils.createTrustStore("classpath:self-signed/ca.crt");
		HttpClient httpClient = createHttpClient(keyStore, "dummy", trustStore);
		RestClient restClient = this.restClientBuilder
				.baseUrl("https://localhost:" + port)
				.requestFactory(new JdkClientHttpRequestFactory(httpClient))
				.build();
		ResponseEntity<String> response = restClient.get()
				.uri("/actuator/health")
				.retrieve()
				.toEntity(String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo("{\"status\":\"UP\"}");
	}

	@Test
	void healthCheckWithoutCertificate() {
		KeyStore keyStore = CertUtils.createEmptyKeyStore();
		KeyStore trustStore = CertUtils.createTrustStore("classpath:self-signed/ca.crt");
		HttpClient httpClient = createHttpClient(keyStore, "dummy", trustStore);
		RestClient restClient = this.restClientBuilder
				.baseUrl("https://localhost:" + port)
				.requestFactory(new JdkClientHttpRequestFactory(httpClient))
				.build();
		try {
			restClient.get()
					.uri("/actuator/health")
					.retrieve()
					.toEntity(String.class);
			fail("Should have thrown an exception");
		} catch (ResourceAccessException e) {
			assertThat(e.getCause()).isInstanceOf(SSLHandshakeException.class);
			assertThat(e.getCause().getMessage()).isEqualTo("Received fatal alert: bad_certificate");
		}
	}

	@Test
	void hello() {
		KeyStore keyStore = CertUtils.createKeyStore("classpath:self-signed/client.key", "classpath:self-signed/client.crt", "dummy");
		KeyStore trustStore = CertUtils.createTrustStore("classpath:self-signed/ca.crt");
		HttpClient httpClient = createHttpClient(keyStore, "dummy", trustStore);
		RestClient restClient = this.restClientBuilder
				.baseUrl("https://localhost:" + port)
				.requestFactory(new JdkClientHttpRequestFactory(httpClient))
				.build();
		ResponseEntity<String> response = restClient.get()
				.uri("/")
				.retrieve()
				.toEntity(String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo("Hello toshiaki-maki!");
	}

	static HttpClient createHttpClient(KeyStore keyStore, String keyStorePassword, KeyStore trustStore) {
		try {
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(keyStore, keyStorePassword.toCharArray());
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(trustStore);
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			return HttpClient.newBuilder().sslContext(sslContext).build();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
