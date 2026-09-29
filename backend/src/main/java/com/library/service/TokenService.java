package com.library.service;

import com.library.config.LibraryProperties;
import com.library.dto.IssuedToken;
import com.library.entity.Librarian;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {

	/** Written to every token's {@code iss} claim and required by the decoder. */
	public static final String ISSUER = "library-api";

	private final JwtEncoder encoder;
	private final LibraryProperties properties;
	private final Clock clock;

	public IssuedToken issue(Librarian librarian) {
		Instant now = clock.instant();
		Instant expiresAt = now.plus(properties.jwt().expiry());
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(ISSUER)
			.subject(librarian.getUsername())
			.claim("name", librarian.getFullName())
			.issuedAt(now)
			.expiresAt(expiresAt)
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new IssuedToken(token, expiresAt);
	}
}
