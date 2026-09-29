package com.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.library.config.LibraryProperties;
import com.library.dto.IssuedToken;
import com.library.entity.Librarian;
import com.library.security.SecurityConfig;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class TokenServiceTest {

	private static final String SECRET = "test-secret-0123456789abcdef-0123456789";

	private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

	private final LibraryProperties properties = new LibraryProperties(new LibraryProperties.Loan(14, 5),
			new LibraryProperties.Jwt(SECRET, Duration.ofHours(8)),
			new LibraryProperties.Admin("admin", "", "Administrator"));

	private final TokenService tokenService = new TokenService(
			new NimbusJwtEncoder(new ImmutableSecret<>(SecurityConfig.secretKey(SECRET))), properties,
			Clock.fixed(now, ZoneOffset.UTC));

	@Test
	void issuesSignedTokenWithSubjectNameAndExpiry() {
		IssuedToken issued = tokenService.issue(new Librarian("jane", "hash", "Jane Doe"));

		Jwt jwt = decoderFor(SECRET).decode(issued.token());

		assertThat(jwt.getSubject()).isEqualTo("jane");
		assertThat(jwt.getClaimAsString("name")).isEqualTo("Jane Doe");
		assertThat(jwt.getIssuedAt()).isEqualTo(now);
		assertThat(jwt.getExpiresAt()).isEqualTo(now.plus(Duration.ofHours(8)));
		assertThat(issued.expiresAt()).isEqualTo(jwt.getExpiresAt());
	}

	@Test
	void tokenSignedWithAnotherSecretIsRejected() {
		IssuedToken issued = tokenService.issue(new Librarian("jane", "hash", "Jane Doe"));

		assertThatThrownBy(() -> decoderFor("another-secret-0123456789abcdef-012345").decode(issued.token()))
			.isInstanceOf(JwtException.class);
	}

	private static NimbusJwtDecoder decoderFor(String secret) {
		return NimbusJwtDecoder.withSecretKey(SecurityConfig.secretKey(secret)).macAlgorithm(MacAlgorithm.HS256).build();
	}
}
