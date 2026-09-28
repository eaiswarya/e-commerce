package com.library.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class LibrarianRepositoryTest {

	@Autowired
	private LibrarianRepository repository;

	@Test
	void findsLibrarianByUsername() {
		repository.save(new Librarian("jane", "hash", "Jane Doe"));

		assertThat(repository.findByUsername("jane")).get()
			.extracting(Librarian::getFullName)
			.isEqualTo("Jane Doe");
		assertThat(repository.findByUsername("nobody")).isEmpty();
	}

	@Test
	void rejectsDuplicateUsername() {
		repository.saveAndFlush(new Librarian("jane", "hash", "Jane Doe"));

		assertThatThrownBy(() -> repository.saveAndFlush(new Librarian("jane", "hash2", "Other Jane")))
			.isInstanceOf(DataIntegrityViolationException.class);
	}
}
