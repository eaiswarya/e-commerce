package com.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.library.dto.BookRequest;
import com.library.dto.BookResponse;
import com.library.dto.PageResponse;
import com.library.entity.Book;
import com.library.exception.BusinessRuleException;
import com.library.exception.NotFoundException;
import com.library.repository.BookRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

	@Mock
	private BookRepository repository;

	@InjectMocks
	private BookService service;

	@Test
	void searchReturnsPageOfResponses() {
		Pageable pageable = PageRequest.of(0, 20);
		Book book = book(7L, 2);
		when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of(book), pageable, 1));

		PageResponse<BookResponse> page = service.search("java", "Programming", true, pageable);

		assertThat(page.content()).containsExactly(BookResponse.from(book));
		assertThat(page.totalElements()).isEqualTo(1);
	}

	@Test
	void getReturnsBook() {
		when(repository.findById(7L)).thenReturn(Optional.of(book(7L, 2)));

		assertThat(service.get(7L).title()).isEqualTo("Effective Java");
	}

	@Test
	void getMissingBookThrowsNotFound() {
		when(repository.findById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(7L)).isInstanceOf(NotFoundException.class).hasMessage("Book 7 not found");
	}

	@Test
	void createNormalisesIsbnAndSaves() {
		when(repository.existsByIsbn("9780134685991")).thenReturn(false);
		when(repository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

		BookResponse created = service.create(request("978-0-13-468599-1", 3));

		ArgumentCaptor<Book> saved = ArgumentCaptor.forClass(Book.class);
		verify(repository).save(saved.capture());
		assertThat(saved.getValue().getIsbn()).isEqualTo("9780134685991");
		assertThat(created.totalCopies()).isEqualTo(3);
		assertThat(created.availableCopies()).isEqualTo(3);
	}

	@Test
	void createRejectsDuplicateIsbn() {
		when(repository.existsByIsbn("9780134685991")).thenReturn(true);

		assertThatThrownBy(() -> service.create(request("9780134685991", 1))).isInstanceOf(BusinessRuleException.class)
			.extracting("code")
			.isEqualTo("DUPLICATE");
		verify(repository, never()).save(any());
	}

	@Test
	void updateChangesDetailsAndCopies() {
		Book book = onLoan(book(7L, 5), 2);
		when(repository.findById(7L)).thenReturn(Optional.of(book));
		when(repository.existsByIsbnAndIdNot("030640615X", 7L)).thenReturn(false);

		BookResponse updated = service.update(7L,
				new BookRequest("0-306-40615-x", "New title", "New author", null, 1999, 4));

		assertThat(updated.isbn()).isEqualTo("030640615X");
		assertThat(updated.title()).isEqualTo("New title");
		assertThat(updated.category()).isNull();
		assertThat(updated.totalCopies()).isEqualTo(4);
		assertThat(updated.availableCopies()).isEqualTo(2);
	}

	@Test
	void updateKeepingOwnIsbnIsAllowed() {
		when(repository.findById(7L)).thenReturn(Optional.of(book(7L, 1)));
		when(repository.existsByIsbnAndIdNot("9780134685991", 7L)).thenReturn(false);

		assertThat(service.update(7L, request("9780134685991", 1)).isbn()).isEqualTo("9780134685991");
	}

	@Test
	void updateRejectsIsbnOfAnotherBook() {
		when(repository.findById(7L)).thenReturn(Optional.of(book(7L, 1)));
		when(repository.existsByIsbnAndIdNot("9780441013593", 7L)).thenReturn(true);

		assertThatThrownBy(() -> service.update(7L, request("9780441013593", 1)))
			.isInstanceOf(BusinessRuleException.class)
			.extracting("code")
			.isEqualTo("DUPLICATE");
	}

	@Test
	void updateRejectsTotalBelowCopiesOnLoan() {
		when(repository.findById(7L)).thenReturn(Optional.of(onLoan(book(7L, 5), 3)));
		when(repository.existsByIsbnAndIdNot("9780134685991", 7L)).thenReturn(false);

		assertThatThrownBy(() -> service.update(7L, request("9780134685991", 2)))
			.isInstanceOf(BusinessRuleException.class)
			.extracting("code")
			.isEqualTo("COPIES_ON_LOAN");
	}

	@Test
	void updateMissingBookThrowsNotFound() {
		when(repository.findById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(7L, request("9780134685991", 1))).isInstanceOf(NotFoundException.class);
	}

	@Test
	void deleteRemovesBookWithNoCopiesOnLoan() {
		Book book = book(7L, 2);
		when(repository.findById(7L)).thenReturn(Optional.of(book));

		service.delete(7L);

		verify(repository).delete(book);
	}

	@Test
	void deleteRejectsBookWithCopiesOnLoan() {
		when(repository.findById(7L)).thenReturn(Optional.of(onLoan(book(7L, 2), 1)));

		assertThatThrownBy(() -> service.delete(7L)).isInstanceOf(BusinessRuleException.class)
			.extracting("code")
			.isEqualTo("HAS_ACTIVE_LOANS");
		verify(repository, never()).delete(any(Book.class));
	}

	@Test
	void deleteMissingBookThrowsNotFound() {
		when(repository.findById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete(7L)).isInstanceOf(NotFoundException.class);
	}

	private static BookRequest request(String isbn, int copies) {
		return new BookRequest(isbn, "Effective Java", "Joshua Bloch", "Programming", 2018, copies);
	}

	private static Book book(Long id, int copies) {
		Book book = new Book("9780134685991", "Effective Java", "Joshua Bloch", "Programming", 2018, copies);
		ReflectionTestUtils.setField(book, "id", id);
		return book;
	}

	private static Book onLoan(Book book, int copies) {
		ReflectionTestUtils.setField(book, "availableCopies", book.getTotalCopies() - copies);
		return book;
	}
}
