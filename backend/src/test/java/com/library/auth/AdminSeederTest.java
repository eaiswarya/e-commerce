package com.library.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.library.common.LibraryProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminSeederTest {

	@Mock
	private LibrarianRepository repository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Test
	void createsAdminWhenNoLibrariansExist() {
		when(repository.count()).thenReturn(0L);
		when(passwordEncoder.encode("admin123")).thenReturn("hashed");

		seeder("admin123").run(null);

		ArgumentCaptor<Librarian> saved = ArgumentCaptor.forClass(Librarian.class);
		verify(repository).save(saved.capture());
		assertThat(saved.getValue().getUsername()).isEqualTo("admin");
		assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed");
		assertThat(saved.getValue().getFullName()).isEqualTo("Administrator");
	}

	@Test
	void skipsWhenLibrariansAlreadyExist() {
		when(repository.count()).thenReturn(1L);

		seeder("admin123").run(null);

		verify(repository, never()).save(any());
	}

	@Test
	void skipsWhenPasswordNotConfigured() {
		when(repository.count()).thenReturn(0L);

		seeder("").run(null);

		verify(repository, never()).save(any());
	}

	private AdminSeeder seeder(String password) {
		var properties = new LibraryProperties(new LibraryProperties.Loan(14, 5),
				new LibraryProperties.Jwt("x".repeat(32), Duration.ofHours(8)),
				new LibraryProperties.Admin("admin", password, "Administrator"));
		return new AdminSeeder(repository, passwordEncoder, properties);
	}
}
