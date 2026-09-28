package com.library.auth;

import com.library.common.LibraryProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http,
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) throws Exception {
		// Route security failures through GlobalExceptionHandler so every error has the same JSON shape.
		AuthenticationEntryPoint entryPoint = (request, response, ex) -> resolver.resolveException(request, response,
				null, ex);
		AccessDeniedHandler deniedHandler = (request, response, ex) -> resolver.resolveException(request, response,
				null, ex);

		http.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin())) // H2 console (dev only)
			.authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.POST, "/api/auth/login")
				.permitAll()
				.requestMatchers("/actuator/health", "/h2-console/**", "/error")
				.permitAll()
				.anyRequest()
				.authenticated())
			.oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()).authenticationEntryPoint(entryPoint))
			.exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler));
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	JwtEncoder jwtEncoder(LibraryProperties properties) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(properties.jwt().secret())));
	}

	@Bean
	JwtDecoder jwtDecoder(LibraryProperties properties) {
		return NimbusJwtDecoder.withSecretKey(secretKey(properties.jwt().secret()))
			.macAlgorithm(MacAlgorithm.HS256)
			.build();
	}

	static SecretKey secretKey(String secret) {
		return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}
}
