package com.library.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.library.entity.Book;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BookRequestTest {

	private static ValidatorFactory factory;
	private static Validator validator;

	@BeforeAll
	static void setUp() {
		factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@AfterAll
	static void tearDown() {
		factory.close();
	}

	@Test
	void acceptsValidRequest() {
		assertThat(violations(request("978-0-13-468599-1", 3))).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "9780134685991", "978 0 13 468599 1", "0306406152", "0-306-40615-X", "030640615x" })
	void acceptsIsbn10And13WithOrWithoutSeparators(String isbn) {
		assertThat(violations(request(isbn, 1))).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "12345", "97801346859911", "978013468599X", "abcdefghij", "978--0134685991", "-9780134685991" })
	void rejectsMalformedIsbn(String isbn) {
		assertThat(violations(request(isbn, 1))).containsExactly("isbn");
	}

	@Test
	void requiresIsbnTitleAuthorAndTotalCopies() {
		BookRequest empty = new BookRequest(" ", " ", " ", null, null, null);

		assertThat(violations(empty)).containsExactlyInAnyOrder("isbn", "title", "author", "totalCopies");
	}

	@Test
	void rejectsOutOfRangeValues() {
		BookRequest request = new BookRequest("9780134685991", "t".repeat(201), "a".repeat(151), "c".repeat(51), 1449,
				-1);

		assertThat(violations(request)).containsExactlyInAnyOrder("title", "author", "category", "publishedYear",
				"totalCopies");
		assertThat(violations(new BookRequest("9780134685991", "T", "A", null, 2101, 1001)))
			.containsExactlyInAnyOrder("publishedYear", "totalCopies");
	}

	@Test
	void responseCopiesEveryField() {
		Book book = new Book("9780134685991", "Effective Java", "Joshua Bloch", "Programming", 2018, 3);

		assertThat(BookResponse.from(book))
			.isEqualTo(new BookResponse(null, "9780134685991", "Effective Java", "Joshua Bloch", "Programming", 2018, 3, 3));
	}

	private static BookRequest request(String isbn, int copies) {
		return new BookRequest(isbn, "Effective Java", "Joshua Bloch", "Programming", 2018, copies);
	}

	private static Set<String> violations(BookRequest request) {
		return validator.validate(request)
			.stream()
			.map(ConstraintViolation::getPropertyPath)
			.map(Object::toString)
			.collect(Collectors.toSet());
	}
}
