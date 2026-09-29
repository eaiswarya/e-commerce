package com.library.repository;

import com.library.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface MemberRepository extends JpaRepository<Member, Long>, JpaSpecificationExecutor<Member> {

	boolean existsByEmail(String email);

	boolean existsByEmailAndIdNot(String email, Long id);

	/** Next number for {@link Member#formatCode}. Works on PostgreSQL and on H2 in PostgreSQL mode. */
	@Query(value = "SELECT nextval('member_code_seq')", nativeQuery = true)
	long nextMemberCodeNumber();
}
