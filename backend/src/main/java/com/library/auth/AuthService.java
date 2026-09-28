package com.library.auth;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final LibrarianRepository repository;

	private final PasswordEncoder passwordEncoder;

	private final TokenService tokenService;

	public AuthService(LibrarianRepository repository, PasswordEncoder passwordEncoder, TokenService tokenService) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
	}

	@Transactional(readOnly = true)
	public LoginResponse login(String username, String password) {
		Librarian librarian = repository.findByUsername(username)
			.filter(l -> passwordEncoder.matches(password, l.getPasswordHash()))
			.orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
		IssuedToken token = tokenService.issue(librarian);
		return new LoginResponse(token.token(), token.expiresAt(), librarian.getFullName());
	}
}
