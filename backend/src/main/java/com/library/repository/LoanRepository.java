package com.library.repository;

import com.library.entity.Loan;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LoanRepository extends JpaRepository<Loan, Long>, JpaSpecificationExecutor<Loan> {

	/** Loads each loan's book and member in the same query, since every response shows them. */
	@Override
	@EntityGraph(attributePaths = { "book", "member" })
	Page<Loan> findAll(Specification<Loan> spec, Pageable pageable);

	long countByMemberIdAndReturnedAtIsNull(Long memberId);

	boolean existsByMemberIdAndReturnedAtIsNullAndDueDateBefore(Long memberId, LocalDate today);

	boolean existsByBookId(Long bookId);
}
