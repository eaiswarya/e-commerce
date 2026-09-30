package com.library.repository;

import com.library.entity.Loan;
import com.library.entity.LoanStatus;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

/**
 * Search filters for loans. A missing value (or {@link LoanStatus#ALL}) means "no filter" and returns
 * {@link Specification#unrestricted()}, so filters can always be combined with {@code allOf}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LoanSpecifications {

	/**
	 * {@code ACTIVE}: not yet returned, overdue loans included. {@code OVERDUE}: not returned and due before
	 * {@code today}. {@code RETURNED}: returned.
	 */
	public static Specification<Loan> hasStatus(LoanStatus status, LocalDate today) {
		if (status == null) {
			return Specification.unrestricted();
		}
		return switch (status) {
			case ACTIVE -> (root, query, cb) -> cb.isNull(root.get("returnedAt"));
			case OVERDUE -> (root, query, cb) -> cb.and(cb.isNull(root.get("returnedAt")),
					cb.lessThan(root.get("dueDate"), today));
			case RETURNED -> (root, query, cb) -> cb.isNotNull(root.get("returnedAt"));
			case ALL -> Specification.unrestricted();
		};
	}

	public static Specification<Loan> forMember(Long memberId) {
		if (memberId == null) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.equal(root.get("member").get("id"), memberId);
	}

	public static Specification<Loan> forBook(Long bookId) {
		if (bookId == null) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.equal(root.get("book").get("id"), bookId);
	}
}
