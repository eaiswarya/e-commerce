package com.library.dto;

import com.library.entity.Book;
import com.library.entity.Loan;
import com.library.entity.LoanStatus;
import com.library.entity.Member;
import java.time.Instant;
import java.time.LocalDate;

/** {@code status} is {@code ACTIVE}, {@code OVERDUE} or {@code RETURNED} as of {@code today}. */
public record LoanResponse(Long id, Long bookId, String bookTitle, String bookIsbn, Long memberId, String memberCode,
		String memberName, Instant borrowedAt, LocalDate dueDate, Instant returnedAt, LoanStatus status) {

	public static LoanResponse from(Loan loan, LocalDate today) {
		Book book = loan.getBook();
		Member member = loan.getMember();
		return new LoanResponse(loan.getId(), book.getId(), book.getTitle(), book.getIsbn(), member.getId(),
				member.getMemberCode(), member.getFullName(), loan.getBorrowedAt(), loan.getDueDate(),
				loan.getReturnedAt(), loan.statusOn(today));
	}
}
