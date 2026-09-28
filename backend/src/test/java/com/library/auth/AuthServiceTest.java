package com.library.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private LibrarianRepository repository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private TokenService tokenService;

	@InjectMocks
	private AuthService authService;

	private final Librarian jane = new Librarian("jane", "hash", "Jane Doe");

	@Test
	void returnsTokenWhenCredentialsMatch() {
		Instant expiry = Instant.parse("2026-01-01T08:00:00Z");
		when(repository.findByUsername("jane")).thenReturn(Optional.of(jane));
		when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
		when(tokenService.issue(jane)).thenReturn(new IssuedToken("tok", expiry));

		LoginResponse response = authService.login("jane", "secret");

		assertThat(response).isEqualTo(new LoginResponse("tok", expiry, "Jane Doe"));
	}

	@Test
	void rejectsWrongPassword() {
		when(repository.findByUsername("jane")).thenReturn(Optional.of(jane));
		when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

		assertThatThrownBy(() -> authService.login("jane", "wrong")).isInstanceOf(BadCredentialsException.class)
			.hasMessage("Invalid username or password");
	}

	@Test
	void rejectsUnknownUserWithSameMessage() {
		when(repository.findByUsername("ghost")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login("ghost", "secret")).isInstanceOf(BadCredentialsException.class)
			.hasMessage("Invalid username or password");
	}
}
