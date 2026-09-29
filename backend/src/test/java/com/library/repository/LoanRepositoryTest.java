package com.library.repository;

import static com.library.repository.LoanSpecifications.forBook;
import static com.library.repository.LoanSpecifications.forMember;
import static com.library.repository.LoanSpecifications.hasStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.library.entity.Book;
import com.library.entity.Loan;
import com.library.entity.LoanStatus;
import com.library.entity.Member;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@DataJpaTest
class LoanRepositoryTest {

	private static final LocalDate TODAY = LocalDate.parse("2026-09-29");
	private static final Instant NOW = Instant.parse("2026-09-29T10:00:00Z");

	@Autowired
	private LoanRepository repository;

	@Autowired
	private BookRepository books;

	@Autowired
	private MemberRepository members;

	@Autowired
	private EntityManager entityManager;

	private Book dune;
	private Book emma;
	private Member ada;
	private Member grace;
	private Loan onTime;
	private Loan overdue;
	private Loan returned;

	@BeforeEach
	void setUp() {
		dune = books.save(new Book("9780441013593", "Dune", "Frank Herbert", null, null, 3));
		emma = books.save(new Book("9780141439587", "Emma", "Jane Austen", null, null, 3));
		ada = members.save(new Member("M0001", "Ada Lovelace", "ada@example.com", null, NOW));
		grace = members.save(new Member("M0002", "Grace Hopper", "grace@example.com", null, NOW));
		onTime = repository.save(new Loan(dune, ada, NOW, TODAY));
		overdue = repository.save(new Loan(emma, ada, NOW.minusSeconds(86_400 * 20), TODAY.minusDays(1)));
		returned = new Loan(dune, grace, NOW.minusSeconds(86_400 * 30), TODAY.minusDays(16));
		returned.markReturned(NOW.minusSeconds(86_400 * 18));
		repository.saveAndFlush(returned);
	}

	@Test
	void activeIncludesOverdueButNotReturned() {
		assertThat(ids(hasStatus(LoanStatus.ACTIVE, TODAY))).containsExactlyInAnyOrder(onTime.getId(), overdue.getId());
	}

	@Test
	void overdueMeansActiveAndDueBeforeToday() {
		assertThat(ids(hasStatus(LoanStatus.OVERDUE, TODAY))).containsExactly(overdue.getId());
	}

	@Test
	void returnedKeepsOnlyReturnedLoans() {
		assertThat(ids(hasStatus(LoanStatus.RETURNED, TODAY))).containsExactly(returned.getId());
	}

	@Test
	void allOrMissingStatusMatchesEverything() {
		assertThat(ids(hasStatus(LoanStatus.ALL, TODAY))).hasSize(3);
		assertThat(ids(Specification.allOf(hasStatus(null, TODAY), forMember(null), forBook(null)))).hasSize(3);
	}

	@Test
	void filtersByMemberAndBook() {
		assertThat(ids(forMember(ada.getId()))).containsExactlyInAnyOrder(onTime.getId(), overdue.getId());
		assertThat(ids(forBook(dune.getId()))).containsExactlyInAnyOrder(onTime.getId(), returned.getId());
		assertThat(ids(Specification.allOf(forMember(ada.getId()), forBook(dune.getId())))).containsExactly(onTime.getId());
	}

	@Test
	void searchLoadsBookAndMemberWithTheLoans() {
		entityManager.clear();

		Page<Loan> page = repository.findAll(forMember(ada.getId()), PageRequest.of(0, 10));

		assertThat(page.getContent()).allSatisfy(loan -> {
			assertThat(Hibernate.isInitialized(loan.getBook())).isTrue();
			assertThat(Hibernate.isInitialized(loan.getMember())).isTrue();
		});
		assertThat(page.getTotalElements()).isEqualTo(2);
	}

	@Test
	void countsActiveLoansPerMember() {
		assertThat(repository.countByMemberIdAndReturnedAtIsNull(ada.getId())).isEqualTo(2);
		assertThat(repository.countByMemberIdAndReturnedAtIsNull(grace.getId())).isZero();
	}

	@Test
	void findsOverdueLoansPerMember() {
		assertThat(repository.existsByMemberIdAndReturnedAtIsNullAndDueDateBefore(ada.getId(), TODAY)).isTrue();
		assertThat(repository.existsByMemberIdAndReturnedAtIsNullAndDueDateBefore(ada.getId(), TODAY.minusDays(1)))
			.isFalse();
		assertThat(repository.existsByMemberIdAndReturnedAtIsNullAndDueDateBefore(grace.getId(), TODAY)).isFalse();
	}

	@Test
	void knowsWhichBooksHaveLoanHistory() {
		Book neverBorrowed = books.save(new Book("0306406152", "Unread", "Nobody", null, null, 1));

		assertThat(repository.existsByBookId(dune.getId())).isTrue();
		assertThat(repository.existsByBookId(neverBorrowed.getId())).isFalse();
	}

	@Test
	void databaseRefusesToDeleteABookWithLoans() {
		// Clear the persistence context so the foreign key in the database, not Hibernate, rejects the delete.
		entityManager.clear();

		assertThatThrownBy(() -> {
			books.delete(books.findById(emma.getId()).orElseThrow());
			books.flush();
		}).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void lockingLookupFindsTheMember() {
		assertThat(members.findWithLockById(ada.getId())).contains(ada);
		assertThat(members.findWithLockById(999_999L)).isEmpty();
	}

	private List<Long> ids(Specification<Loan> spec) {
		return repository.findAll(spec, Pageable.unpaged(Sort.by("id"))).map(Loan::getId).getContent();
	}
}
