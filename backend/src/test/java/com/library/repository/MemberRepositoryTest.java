package com.library.repository;

import static com.library.repository.MemberSpecifications.hasActive;
import static com.library.repository.MemberSpecifications.matchesQuery;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.library.entity.Member;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@DataJpaTest
class MemberRepositoryTest {

	private static final Instant JOINED = Instant.parse("2026-09-29T10:00:00Z");

	@Autowired
	private MemberRepository repository;

	private Member ada;
	private Member grace;

	@BeforeEach
	void setUp() {
		ada = repository.save(new Member("M0001", "Ada Lovelace", "ada@example.com", null, JOINED));
		grace = repository.save(new Member("M0002", "Grace Hopper", "grace@navy.example", "555-0100", JOINED));
		Member alan = repository.save(new Member("M0003", "Alan Turing", "alan@example.com", null, JOINED));
		alan.deactivate();
		repository.saveAndFlush(alan);
	}

	@Test
	void queryMatchesNameIgnoringCase() {
		assertThat(names(matchesQuery("LOVE"))).containsExactly("Ada Lovelace");
	}

	@Test
	void queryMatchesEmailFragment() {
		assertThat(names(matchesQuery("navy"))).containsExactly("Grace Hopper");
	}

	@Test
	void queryMatchesMemberCodeIgnoringCase() {
		assertThat(names(matchesQuery("m0002"))).containsExactly("Grace Hopper");
	}

	@Test
	void queryMatchesWholeMemberCodeOnly() {
		assertThat(names(matchesQuery("M000"))).isEmpty();
	}

	@Test
	void queryTreatsLikeWildcardsLiterally() {
		assertThat(names(matchesQuery("%"))).isEmpty();
		assertThat(names(matchesQuery("_"))).isEmpty();
	}

	@Test
	void activeTrueKeepsOnlyActiveMembers() {
		assertThat(names(hasActive(true))).containsExactly("Ada Lovelace", "Grace Hopper");
	}

	@Test
	void activeFalseKeepsOnlyInactiveMembers() {
		assertThat(names(hasActive(false))).containsExactly("Alan Turing");
	}

	@Test
	void filtersCombine() {
		assertThat(names(Specification.allOf(matchesQuery("example.com"), hasActive(true)))).containsExactly("Ada Lovelace");
	}

	@Test
	void blankOrMissingFiltersMatchEverything() {
		assertThat(names(Specification.allOf(matchesQuery("  "), hasActive(null))))
			.containsExactly("Ada Lovelace", "Alan Turing", "Grace Hopper");
	}

	@Test
	void findsDuplicateEmailOnOtherMembersOnly() {
		assertThat(repository.existsByEmail("grace@navy.example")).isTrue();
		assertThat(repository.existsByEmail("nobody@example.com")).isFalse();
		assertThat(repository.existsByEmailAndIdNot("grace@navy.example", grace.getId())).isFalse();
		assertThat(repository.existsByEmailAndIdNot("grace@navy.example", ada.getId())).isTrue();
	}

	@Test
	void rejectsDuplicateEmail() {
		assertThatThrownBy(() -> repository.saveAndFlush(new Member("M0009", "Ada Two", "ada@example.com", null, JOINED)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsDuplicateMemberCode() {
		assertThatThrownBy(() -> repository.saveAndFlush(new Member("M0001", "Someone", "someone@example.com", null, JOINED)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void memberCodeNumbersIncrease() {
		long first = repository.nextMemberCodeNumber();
		long second = repository.nextMemberCodeNumber();

		assertThat(first).isPositive();
		assertThat(second).isGreaterThan(first);
	}

	@Test
	void storesJoinedAtAndVersion() {
		repository.flush();
		Member found = repository.findById(ada.getId()).orElseThrow();

		assertThat(found.getJoinedAt()).isEqualTo(JOINED);
		assertThat(found.getVersion()).isZero();
	}

	private List<String> names(Specification<Member> spec) {
		return repository.findAll(spec, Pageable.unpaged(Sort.by("fullName"))).map(Member::getFullName).getContent();
	}
}
