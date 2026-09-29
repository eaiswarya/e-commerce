package com.library.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.library.exception.BusinessRuleException;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LoanTest {

	private static final Instant BORROWED = Instant.parse("2026-09-01T09:00:00Z");
	private static final LocalDate DUE = LocalDate.parse("2026-09-15");

	@Test
	void newLoanIsActive() {
		Loan loan = loan();

		assertThat(loan.isActive()).isTrue();
		assertThat(loan.getReturnedAt()).isNull();
		assertThat(loan.statusOn(DUE)).isEqualTo(LoanStatus.ACTIVE);
	}

	@Test
	void loanDueTodayIsNotOverdue() {
		assertThat(loan().isOverdue(DUE)).isFalse();
	}

	@Test
	void loanPastItsDueDateIsOverdue() {
		Loan loan = loan();

		assertThat(loan.isOverdue(DUE.plusDays(1))).isTrue();
		assertThat(loan.statusOn(DUE.plusDays(1))).isEqualTo(LoanStatus.OVERDUE);
	}

	@Test
	void returningRecordsTheTime() {
		Loan loan = loan();
		Instant returned = Instant.parse("2026-09-20T12:00:00Z");

		loan.markReturned(returned);

		assertThat(loan.isActive()).isFalse();
		assertThat(loan.getReturnedAt()).isEqualTo(returned);
		assertThat(loan.isOverdue(DUE.plusDays(10))).isFalse();
		assertThat(loan.statusOn(DUE.plusDays(10))).isEqualTo(LoanStatus.RETURNED);
	}

	@Test
	void returningTwiceIsRejected() {
		Loan loan = loan();
		Instant first = Instant.parse("2026-09-10T12:00:00Z");
		loan.markReturned(first);

		assertThatThrownBy(() -> loan.markReturned(first.plusSeconds(60))).isInstanceOf(BusinessRuleException.class)
			.extracting("code")
			.isEqualTo("ALREADY_RETURNED");
		assertThat(loan.getReturnedAt()).isEqualTo(first);
	}

	private static Loan loan() {
		Book book = new Book("9780134685991", "Effective Java", "Joshua Bloch", null, null, 1);
		Member member = new Member("M0001", "Ada Lovelace", "ada@example.com", null, BORROWED);
		return new Loan(book, member, BORROWED, DUE);
	}
}
