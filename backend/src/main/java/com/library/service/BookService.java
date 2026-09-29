package com.library.service;

import static com.library.repository.BookSpecifications.hasCategory;
import static com.library.repository.BookSpecifications.isAvailable;
import static com.library.repository.BookSpecifications.matchesQuery;

import com.library.dto.BookRequest;
import com.library.dto.BookResponse;
import com.library.dto.PageResponse;
import com.library.entity.Book;
import com.library.exception.BusinessRuleException;
import com.library.exception.NotFoundException;
import com.library.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

	private final BookRepository repository;

	public PageResponse<BookResponse> search(String q, String category, Boolean available, Pageable pageable) {
		Specification<Book> spec = Specification.allOf(matchesQuery(q), hasCategory(category), isAvailable(available));
		return PageResponse.from(repository.findAll(spec, pageable).map(BookResponse::from));
	}

	public BookResponse get(Long id) {
		return BookResponse.from(find(id));
	}

	@Transactional
	public BookResponse create(BookRequest request) {
		String isbn = Book.normaliseIsbn(request.isbn());
		if (repository.existsByIsbn(isbn)) {
			throw duplicateIsbn(isbn);
		}
		Book book = new Book(isbn, request.title(), request.author(), request.category(), request.publishedYear(),
				request.totalCopies());
		return BookResponse.from(repository.save(book));
	}

	@Transactional
	public BookResponse update(Long id, BookRequest request) {
		Book book = find(id);
		if (!book.getVersion().equals(request.version())) {
			// The client edited an out-of-date copy; GlobalExceptionHandler answers 409 CONCURRENT_UPDATE.
			throw new ObjectOptimisticLockingFailureException(Book.class, id);
		}
		String isbn = Book.normaliseIsbn(request.isbn());
		if (repository.existsByIsbnAndIdNot(isbn, id)) {
			throw duplicateIsbn(isbn);
		}
		book.updateDetails(isbn, request.title(), request.author(), request.category(), request.publishedYear());
		book.changeTotalCopies(request.totalCopies());
		// Flush now so the response carries the incremented version the client must send next time.
		return BookResponse.from(repository.saveAndFlush(book));
	}

	@Transactional
	public void delete(Long id) {
		Book book = find(id);
		if (book.hasCopiesOnLoan()) {
			throw new BusinessRuleException("HAS_ACTIVE_LOANS",
					"Book %d has %d copies on loan".formatted(id, book.getCopiesOnLoan()));
		}
		repository.delete(book);
	}

	private Book find(Long id) {
		return repository.findById(id).orElseThrow(() -> new NotFoundException("Book %d not found".formatted(id)));
	}

	private static BusinessRuleException duplicateIsbn(String isbn) {
		return new BusinessRuleException("DUPLICATE", "A book with ISBN %s already exists".formatted(isbn));
	}
}
