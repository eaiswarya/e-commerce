package com.library.service;

import com.library.dto.IssuedToken;
import com.library.dto.LoginResponse;
import com.library.entity.Librarian;
import com.library.repository.LibrarianRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final LibrarianRepository repository;

	private final PasswordEncoder passwordEncoder;

	private final TokenService tokenService;

	/** Checked when the username is unknown, so response time doesn't reveal which usernames exist. */
	private final String dummyHash;

	public AuthService(LibrarianRepository repository, PasswordEncoder passwordEncoder, TokenService tokenService) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
		this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
	}

	@Transactional(readOnly = true)
	public LoginResponse login(String username, String password) {
		Optional<Librarian> found = repository.findByUsername(username);
		boolean matches = passwordEncoder.matches(password, found.map(Librarian::getPasswordHash).orElse(dummyHash));
		if (found.isEmpty() || !matches) {
			throw new BadCredentialsException("Invalid username or password");
		}
		Librarian librarian = found.get();
		IssuedToken token = tokenService.issue(librarian);
		return new LoginResponse(token.token(), token.expiresAt(), librarian.getFullName());
	}
}
