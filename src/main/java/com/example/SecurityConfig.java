package com.example;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		return http
				.authorizeHttpRequests(authz -> authz
						.requestMatchers("/").hasRole("MTLS")
						.anyRequest().permitAll())
				.x509(s -> s.subjectPrincipalRegex("CN=([\\w\\-]+)"))
				.build();
	}

	@Bean
	public UserDetailsService userDetailsService() {
		return username -> User.withUsername(username).password("{noop}dummy").roles("MTLS").build();
	}
}
