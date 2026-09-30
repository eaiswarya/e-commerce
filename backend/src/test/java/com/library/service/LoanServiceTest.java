package com.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.library.config.LibraryProperties;
import com.library.dto.BorrowRequest;
import com.library.dto.LoanResponse;
import com.library.dto.PageResponse;
import com.library.entity.Book;
import com.library.entity.Loan;
import com.library.entity.LoanStatus;
import com.library.entity.Member;
import com.library.exception.BusinessRuleException;
import com.library.exception.NotFoundException;
import com.library.repository.BookRepository;
import com.library.repository.LoanRepository;
import com.library.repository.MemberRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

	private static final Instant NOW = Instant.parse("2026-09-29T10:15:30Z");
	private static final LocalDate TODAY = LocalDate.parse("2026-09-29");
	private static final int PERIOD_DAYS = 21;
	private static final int MAX_ACTIVE = 3;

	@Mock
	private LoanRepository loans;

	@Mock
	private BookRepository books;

	@Mock
	private MemberRepository members;

	private LoanService service;

	private Book book;
	private Member member;

	@BeforeEach
	void setUp() {
		// Deliberately not the defaults (14 / 5), so the tests prove the values come from configuration.
		LibraryProperties properties = new LibraryProperties(new LibraryProperties.Loan(PERIOD_DAYS, MAX_ACTIVE),
				new LibraryProperties.Jwt("x".repeat(32), Duration.ofHours(8)),
				new LibraryProperties.Admin("admin", "", "Administrator"));
		service = new LoanService(loans, books, members, properties, Clock.fixed(NOW, ZoneOffset.UTC));
		book = new Book("9780134685991", "Effective Java", "Joshua Bloch", null, null, 2);
		ReflectionTestUtils.setField(book, "id", 3L);
		member = new Member("M0007", "Ada Lovelace", "ada@example.com", null, NOW);
		ReflectionTestUtils.setField(member, "id", 7L);
	}

	@Test
	void borrowTakesACopyAndSetsDueDateFromConfig() {
		allowBorrow();
		when(loans.save(any(Loan.class))).thenAnswer(inv -> withId(inv.getArgument(0), 11L));

		LoanResponse loan = service.borrow(new BorrowRequest(3L, 7L));

		assertThat(book.getAvailableCopies()).isEqualTo(1);
		assertThat(loan.id()).isEqualTo(11L);
		assertThat(loan.borrowedAt()).isEqualTo(NOW);
		assertThat(loan.dueDate()).isEqualTo(TODAY.plusDays(PERIOD_DAYS));
		assertThat(loan.status()).isEqualTo(LoanStatus.ACTIVE);
		assertThat(loan.memberCode()).isEqualTo("M0007");
		assertThat(loan.bookTitle()).isEqualTo("Effective Java");
		verify(members).findWithLockById(7L);
	}

	@Test
	void borrowByMissingMemberIsNotFound() {
		when(members.findWithLockById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.borrow(new BorrowRequest(3L, 7L))).isInstanceOf(NotFoundException.class)
			.hasMessage("Member 7 not found");
	}

	@Test
	void borrowOfMissingBookIsNotFound() {
		when(members.findWithLockById(7L)).thenReturn(Optional.of(member));
		when(books.findById(3L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.borrow(new BorrowRequest(3L, 7L))).isInstanceOf(NotFoundException.class)
			.hasMessage("Book 3 not found");
	}

	@Test
	void inactiveMemberCannotBorrow() {
		member.deactivate();
		when(members.findWithLockById(7L)).thenReturn(Optional.of(member));
		when(books.findById(3L)).thenReturn(Optional.of(book));

		assertRejected("MEMBER_INACTIVE");
	}

	@Test
	void memberWithOverdueLoanCannotBorrow() {
		when(members.findWithLockById(7L)).thenReturn(Optional.of(member));
		when(books.findById(3L)).thenReturn(Optional.of(book));
		when(loans.existsByMemberIdAndReturnedAtIsNullAndDueDateBefore(7L, TODAY)).thenReturn(true);

		assertRejected("MEMBER_HAS_OVERDUE");
	}

	@Test
	void memberAtTheConfiguredLimitCannotBorrow() {
		when(members.findWithLockById(7L)).thenReturn(Optional.of(member));
		when(books.findById(3L)).thenReturn(Optional.of(book));
		when(loans.countByMemberIdAndReturnedAtIsNull(7L)).thenReturn((long) MAX_ACTIVE);

		assertRejected("LOAN_LIMIT_REACHED");
	}

	@Test
	void memberJustBelowTheLimitCanBorrow() {
		allowBorrow();
		when(loans.countByMemberIdAndReturnedAtIsNull(7L)).thenReturn((long) MAX_ACTIVE - 1);
		when(loans.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

		assertThat(service.borrow(new BorrowRequest(3L, 7L)).status()).isEqualTo(LoanStatus.ACTIVE);
	}

	@Test
	void bookWithNoFreeCopyCannotBeBorrowed() {
		ReflectionTestUtils.setField(book, "availableCopies", 0);
		allowBorrow();

		assertRejected("NO_COPIES_AVAILABLE");
	}

	@Test
	void returnPutsTheCopyBack() {
		book.borrowCopy();
		Loan loan = withId(new Loan(book, member, NOW.minus(Duration.ofDays(3)), TODAY.plusDays(11)), 11L);
		when(loans.findById(11L)).thenReturn(Optional.of(loan));

		LoanResponse returned = service.returnLoan(11L);

		assertThat(returned.returnedAt()).isEqualTo(NOW);
		assertThat(returned.status()).isEqualTo(LoanStatus.RETURNED);
		assertThat(book.getAvailableCopies()).isEqualTo(2);
	}

	@Test
	void secondReturnIsRejectedAndLeavesCopiesAlone() {
		book.borrowCopy();
		Loan loan = withId(new Loan(book, member, NOW.minus(Duration.ofDays(3)), TODAY.plusDays(11)), 11L);
		when(loans.findById(11L)).thenReturn(Optional.of(loan));
		service.returnLoan(11L);

		assertThatThrownBy(() -> service.returnLoan(11L)).isInstanceOf(BusinessRuleException.class)
			.extracting("code")
			.isEqualTo("ALREADY_RETURNED");
		assertThat(book.getAvailableCopies()).isEqualTo(2);
	}

	@Test
	void returnOfMissingLoanIsNotFound() {
		when(loans.findById(11L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.returnLoan(11L)).isInstanceOf(NotFoundException.class)
			.hasMessage("Loan 11 not found");
	}

	@Test
	void getReportsOverdueStatus() {
		Loan loan = withId(new Loan(book, member, NOW.minus(Duration.ofDays(20)), TODAY.minusDays(1)), 11L);
		when(loans.findById(11L)).thenReturn(Optional.of(loan));

		assertThat(service.get(11L).status()).isEqualTo(LoanStatus.OVERDUE);
	}

	@Test
	void searchReturnsPageOfResponses() {
		Pageable pageable = PageRequest.of(0, 20);
		Loan loan = withId(new Loan(book, member, NOW, TODAY.plusDays(14)), 11L);
		when(loans.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of(loan), pageable, 1));

		PageResponse<LoanResponse> page = service.search(LoanStatus.ACTIVE, 7L, 3L, pageable);

		assertThat(page.content()).extracting(LoanResponse::id).containsExactly(11L);
		assertThat(page.totalElements()).isEqualTo(1);
	}

	@Test
	void memberLoansOfMissingMemberIsNotFound() {
		when(members.existsById(7L)).thenReturn(false);

		assertThatThrownBy(() -> service.memberLoans(7L, LoanStatus.ALL, PageRequest.of(0, 20)))
			.isInstanceOf(NotFoundException.class)
			.hasMessage("Member 7 not found");
		verify(loans, never()).findAll(any(Specification.class), any(Pageable.class));
	}

	@Test
	void memberLoansReturnsPage() {
		Pageable pageable = PageRequest.of(0, 20);
		when(members.existsById(7L)).thenReturn(true);
		when(loans.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of(), pageable, 0));

		assertThat(service.memberLoans(7L, null, pageable).totalElements()).isZero();
	}

	private void allowBorrow() {
		when(members.findWithLockById(7L)).thenReturn(Optional.of(member));
		when(books.findById(3L)).thenReturn(Optional.of(book));
	}

	private void assertRejected(String code) {
		assertThatThrownBy(() -> service.borrow(new BorrowRequest(3L, 7L))).isInstanceOf(BusinessRuleException.class)
			.extracting("code")
			.isEqualTo(code);
		verify(loans, never()).save(any(Loan.class));
	}

	private static Loan withId(Loan loan, Long id) {
		ReflectionTestUtils.setField(loan, "id", id);
		return loan;
	}
}
