package com.library.repository;

import static com.library.repository.BookSpecifications.hasCategory;
import static com.library.repository.BookSpecifications.isAvailable;
import static com.library.repository.BookSpecifications.matchesQuery;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.library.entity.Book;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
class BookRepositoryTest {

	@Autowired
	private BookRepository repository;

	private Book effectiveJava;
	private Book cleanCode;
	private Book dune;

	@BeforeEach
	void setUp() {
		effectiveJava = repository.save(new Book("9780134685991", "Effective Java", "Joshua Bloch", "Programming", 2018, 2));
		cleanCode = repository.save(new Book("9780132350884", "Clean Code", "Robert C. Martin", "Programming", 2008, 1));
		dune = repository.save(new Book("9780441013593", "Dune", "Frank Herbert", "Fiction", 1965, 1));
		ReflectionTestUtils.setField(cleanCode, "availableCopies", 0);
		repository.saveAndFlush(cleanCode);
	}

	@Test
	void queryMatchesTitleIgnoringCase() {
		assertThat(titles(matchesQuery("effECTive"))).containsExactly("Effective Java");
	}

	@Test
	void queryMatchesAuthorFragment() {
		assertThat(titles(matchesQuery("herb"))).containsExactly("Dune");
	}

	@Test
	void queryMatchesIsbnWrittenWithHyphens() {
		assertThat(titles(matchesQuery("978-0-13-235088-4"))).containsExactly("Clean Code");
	}

	@Test
	void queryTreatsLikeWildcardsLiterally() {
		assertThat(titles(matchesQuery("%"))).isEmpty();
		assertThat(titles(matchesQuery("_"))).isEmpty();
	}

	@Test
	void categoryMatchesIgnoringCase() {
		assertThat(titles(hasCategory("programming"))).containsExactly("Clean Code", "Effective Java");
	}

	@Test
	void availableKeepsOnlyBooksWithFreeCopies() {
		assertThat(titles(isAvailable(true))).containsExactly("Dune", "Effective Java");
	}

	@Test
	void filtersCombine() {
		assertThat(titles(Specification.allOf(matchesQuery("c"), hasCategory("Programming"), isAvailable(true))))
			.containsExactly("Effective Java");
	}

	@Test
	void blankOrMissingFiltersMatchEverything() {
		assertThat(titles(Specification.allOf(matchesQuery("  "), hasCategory(null), isAvailable(null))))
			.containsExactly("Clean Code", "Dune", "Effective Java");
		assertThat(titles(isAvailable(false))).containsExactly("Clean Code", "Dune", "Effective Java");
	}

	@Test
	void findsDuplicateIsbnOnOtherBooksOnly() {
		assertThat(repository.existsByIsbn("9780441013593")).isTrue();
		assertThat(repository.existsByIsbn("0000000000")).isFalse();
		assertThat(repository.existsByIsbnAndIdNot("9780441013593", dune.getId())).isFalse();
		assertThat(repository.existsByIsbnAndIdNot("9780441013593", effectiveJava.getId())).isTrue();
	}

	@Test
	void rejectsDuplicateIsbn() {
		assertThatThrownBy(() -> repository.saveAndFlush(new Book("9780441013593", "Dune (copy)", "F. Herbert", null, null, 1)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void databaseRejectsMoreAvailableThanTotalCopies() {
		ReflectionTestUtils.setField(dune, "availableCopies", 5);

		assertThatThrownBy(() -> repository.saveAndFlush(dune)).isInstanceOf(DataIntegrityViolationException.class);
	}

	private List<String> titles(Specification<Book> spec) {
		return repository.findAll(spec, Pageable.unpaged(Sort.by("title"))).map(Book::getTitle).getContent();
	}
}
