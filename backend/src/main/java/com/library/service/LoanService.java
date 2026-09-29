package com.library.service;

import static com.library.repository.LoanSpecifications.forBook;
import static com.library.repository.LoanSpecifications.forMember;
import static com.library.repository.LoanSpecifications.hasStatus;

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
import java.time.Instant;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanService {

	private final LoanRepository loans;

	private final BookRepository books;

	private final MemberRepository members;

	private final LibraryProperties properties;

	private final Clock clock;

	public PageResponse<LoanResponse> search(LoanStatus status, Long memberId, Long bookId, Pageable pageable) {
		return page(Specification.allOf(hasStatus(status, today()), forMember(memberId), forBook(bookId)), pageable);
	}

	public PageResponse<LoanResponse> memberLoans(Long memberId, LoanStatus status, Pageable pageable) {
		if (!members.existsById(memberId)) {
			throw memberNotFound(memberId);
		}
		return page(Specification.allOf(hasStatus(status, today()), forMember(memberId)), pageable);
	}

	public LoanResponse get(Long id) {
		return LoanResponse.from(find(id), today());
	}

	/**
	 * The member's row is locked first, so concurrent borrows by one member cannot both pass the limit check. Two
	 * members taking the last copy at once are caught by the book's {@code @Version}: the second gets 409
	 * {@code CONCURRENT_UPDATE}.
	 */
	@Transactional
	public LoanResponse borrow(BorrowRequest request) {
		Member member = members.findWithLockById(request.memberId())
			.orElseThrow(() -> memberNotFound(request.memberId()));
		Book book = books.findById(request.bookId())
			.orElseThrow(() -> new NotFoundException("Book %d not found".formatted(request.bookId())));
		LocalDate today = today();
		if (!member.isActive()) {
			throw new BusinessRuleException("MEMBER_INACTIVE",
					"Member %s is inactive".formatted(member.getMemberCode()));
		}
		if (loans.existsByMemberIdAndReturnedAtIsNullAndDueDateBefore(member.getId(), today)) {
			throw new BusinessRuleException("MEMBER_HAS_OVERDUE",
					"Member %s has overdue loans".formatted(member.getMemberCode()));
		}
		int maxActive = properties.loan().maxActive();
		if (loans.countByMemberIdAndReturnedAtIsNull(member.getId()) >= maxActive) {
			throw new BusinessRuleException("LOAN_LIMIT_REACHED",
					"Member %s already has %d active loans".formatted(member.getMemberCode(), maxActive));
		}
		book.borrowCopy();
		Loan loan = new Loan(book, member, Instant.now(clock), today.plusDays(properties.loan().periodDays()));
		return LoanResponse.from(loans.save(loan), today);
	}

	@Transactional
	public LoanResponse returnLoan(Long id) {
		Loan loan = find(id);
		loan.markReturned(Instant.now(clock));
		loan.getBook().returnCopy();
		return LoanResponse.from(loan, today());
	}

	private PageResponse<LoanResponse> page(Specification<Loan> spec, Pageable pageable) {
		LocalDate today = today();
		return PageResponse.from(loans.findAll(spec, pageable).map(loan -> LoanResponse.from(loan, today)));
	}

	private Loan find(Long id) {
		return loans.findById(id).orElseThrow(() -> new NotFoundException("Loan %d not found".formatted(id)));
	}

	private LocalDate today() {
		return LocalDate.now(clock);
	}

	private static NotFoundException memberNotFound(Long id) {
		return new NotFoundException("Member %d not found".formatted(id));
	}
}
