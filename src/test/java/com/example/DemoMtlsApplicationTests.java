package com.example;

import javax.net.ssl.SSLHandshakeException;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.client.RestClientSsl;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"spring.profiles.active=tls,mtls",
		"spring.ssl.bundle.pem.client.keystore.certificate=classpath:self-signed/client.crt",
		"spring.ssl.bundle.pem.client.keystore.private-key=classpath:self-signed/client.key",
		"spring.ssl.bundle.pem.client.truststore.certificate=classpath:self-signed/ca.crt",
		"spring.ssl.bundle.pem.cacert.truststore.certificate=classpath:self-signed/ca.crt"
})
class DemoMtlsApplicationTests {
	@LocalServerPort int port;

	@Autowired RestClient.Builder restClientBuilder;

	@Autowired RestClientSsl clientSsl;

	@Test
	void healthCheckWithValidCertificate() {
		RestClient restClient = this.restClientBuilder
				.baseUrl("https://localhost:" + this.port)
				.apply(this.clientSsl.fromBundle("client"))
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
		RestClient restClient = this.restClientBuilder
				.baseUrl("https://localhost:" + this.port)
				.apply(this.clientSsl.fromBundle("cacert"))
				.build();
		try {
			restClient.get()
					.uri("/actuator/health")
					.retrieve()
					.toEntity(String.class);
			fail("Should have thrown an exception");
		}
		catch (ResourceAccessException e) {
			assertThat(e.getCause()).isInstanceOf(SSLHandshakeException.class);
			assertThat(e.getCause().getMessage()).isEqualTo("Received fatal alert: bad_certificate");
		}
	}

	@Test
	void hello() {
		RestClient restClient = this.restClientBuilder
				.baseUrl("https://localhost:" + this.port)
				.apply(this.clientSsl.fromBundle("client"))
				.build();
		ResponseEntity<String> response = restClient.get()
				.uri("/")
				.retrieve()
				.toEntity(String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo("Hello toshiaki-maki!");
	}

}
