package com.library.repository;

import com.library.entity.Member;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface MemberRepository extends JpaRepository<Member, Long>, JpaSpecificationExecutor<Member> {

	/**
	 * Locks the member's row until the transaction ends, so two borrows for the same member run one after the
	 * other and cannot both pass the loan-limit check.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Member> findWithLockById(Long id);

	boolean existsByEmail(String email);

	boolean existsByEmailAndIdNot(String email, Long id);

	/** Next number for {@link Member#formatCode}. Works on PostgreSQL and on H2 in PostgreSQL mode. */
	@Query(value = "SELECT nextval('member_code_seq')", nativeQuery = true)
	long nextMemberCodeNumber();
}
