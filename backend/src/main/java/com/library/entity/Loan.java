package com.library.entity;

import com.library.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One copy of a book lent to a member. Active while {@code returnedAt} is null. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Loan {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "book_id", nullable = false, updatable = false)
	private Book book;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private Member member;

	@Column(name = "borrowed_at", nullable = false, updatable = false)
	private Instant borrowedAt;

	@Column(name = "due_date", nullable = false, updatable = false)
	private LocalDate dueDate;

	@Column(name = "returned_at")
	private Instant returnedAt;

	@Version
	@Column(nullable = false)
	private Long version;

	public Loan(Book book, Member member, Instant borrowedAt, LocalDate dueDate) {
		this.book = book;
		this.member = member;
		this.borrowedAt = borrowedAt;
		this.dueDate = dueDate;
	}

	public boolean isActive() {
		return returnedAt == null;
	}

	/** A loan due today is not yet overdue. */
	public boolean isOverdue(LocalDate today) {
		return isActive() && dueDate.isBefore(today);
	}

	public LoanStatus statusOn(LocalDate today) {
		if (!isActive()) {
			return LoanStatus.RETURNED;
		}
		return isOverdue(today) ? LoanStatus.OVERDUE : LoanStatus.ACTIVE;
	}

	public void markReturned(Instant when) {
		if (!isActive()) {
			throw new BusinessRuleException("ALREADY_RETURNED", "Loan %d was already returned".formatted(id));
		}
		this.returnedAt = when;
	}
}
