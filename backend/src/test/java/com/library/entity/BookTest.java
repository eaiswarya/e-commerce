package com.library.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.library.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class BookTest {

	@Test
	void newBookHasAllCopiesAvailable() {
		Book book = book(3);

		assertThat(book.getTotalCopies()).isEqualTo(3);
		assertThat(book.getAvailableCopies()).isEqualTo(3);
		assertThat(book.hasCopiesOnLoan()).isFalse();
	}

	@Test
	void reportsCopiesOnLoan() {
		Book book = withOnLoan(book(3), 1);

		assertThat(book.getCopiesOnLoan()).isEqualTo(1);
		assertThat(book.hasCopiesOnLoan()).isTrue();
	}

	@Test
	void increasingTotalAddsAvailableCopies() {
		Book book = withOnLoan(book(3), 2);

		book.changeTotalCopies(5);

		assertThat(book.getTotalCopies()).isEqualTo(5);
		assertThat(book.getAvailableCopies()).isEqualTo(3);
	}

	@Test
	void decreasingTotalRemovesAvailableCopies() {
		Book book = withOnLoan(book(5), 2);

		book.changeTotalCopies(2);

		assertThat(book.getTotalCopies()).isEqualTo(2);
		assertThat(book.getAvailableCopies()).isZero();
	}

	@Test
	void rejectsTotalBelowCopiesOnLoan() {
		Book book = withOnLoan(book(5), 3);

		assertThatThrownBy(() -> book.changeTotalCopies(2)).isInstanceOf(BusinessRuleException.class)
			.extracting("code")
			.isEqualTo("COPIES_ON_LOAN");
		assertThat(book.getTotalCopies()).isEqualTo(5);
		assertThat(book.getAvailableCopies()).isEqualTo(2);
	}

	@Test
	void updatesDetails() {
		Book book = book(1);

		book.updateDetails("9780134685991", "Effective Java 3e", "J. Bloch", "Programming", 2018);

		assertThat(book.getIsbn()).isEqualTo("9780134685991");
		assertThat(book.getTitle()).isEqualTo("Effective Java 3e");
		assertThat(book.getAuthor()).isEqualTo("J. Bloch");
		assertThat(book.getCategory()).isEqualTo("Programming");
		assertThat(book.getPublishedYear()).isEqualTo(2018);
	}

	@Test
	void normalisesIsbnByStrippingSeparatorsAndUppercasingX() {
		assertThat(Book.normaliseIsbn(" 978-0-13\t468599-1 ")).isEqualTo("9780134685991");
		assertThat(Book.normaliseIsbn("0-306-40615-x")).isEqualTo("030640615X");
		assertThat(Book.normaliseIsbn(null)).isNull();
	}

	@Test
	void borrowingTakesOneAvailableCopy() {
		Book book = book(2);

		book.borrowCopy();

		assertThat(book.getAvailableCopies()).isEqualTo(1);
		assertThat(book.getCopiesOnLoan()).isEqualTo(1);
	}

	@Test
	void borrowingWithNoFreeCopyIsRejected() {
		Book book = withOnLoan(book(1), 1);

		assertThatThrownBy(book::borrowCopy).isInstanceOf(BusinessRuleException.class)
			.extracting("code")
			.isEqualTo("NO_COPIES_AVAILABLE");
		assertThat(book.getAvailableCopies()).isZero();
	}

	@Test
	void returningPutsTheCopyBack() {
		Book book = withOnLoan(book(2), 1);

		book.returnCopy();

		assertThat(book.getAvailableCopies()).isEqualTo(2);
		assertThat(book.hasCopiesOnLoan()).isFalse();
	}

	private static Book book(int copies) {
		return new Book("9780134685991", "Effective Java", "Joshua Bloch", "Programming", 2018, copies);
	}

	/** Simulates copies on loan by lowering availableCopies directly. */
	private static Book withOnLoan(Book book, int onLoan) {
		ReflectionTestUtils.setField(book, "availableCopies", book.getTotalCopies() - onLoan);
		return book;
	}
}
