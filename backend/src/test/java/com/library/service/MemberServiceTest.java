package com.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.library.dto.MemberRequest;
import com.library.dto.MemberResponse;
import com.library.dto.PageResponse;
import com.library.entity.Member;
import com.library.exception.BusinessRuleException;
import com.library.exception.NotFoundException;
import com.library.repository.MemberRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

	private static final Instant NOW = Instant.parse("2026-09-29T10:15:30Z");
	private static final long VERSION = 3L;

	@Mock
	private MemberRepository repository;

	private MemberService service;

	@BeforeEach
	void setUp() {
		service = new MemberService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void searchReturnsPageOfResponses() {
		Pageable pageable = PageRequest.of(0, 20);
		Member member = member(7L);
		when(repository.findAll(any(Specification.class), eq(pageable)))
			.thenReturn(new PageImpl<>(List.of(member), pageable, 1));

		PageResponse<MemberResponse> page = service.search("ada", true, pageable);

		assertThat(page.content()).containsExactly(MemberResponse.from(member));
		assertThat(page.totalElements()).isEqualTo(1);
	}

	@Test
	void getReturnsMember() {
		when(repository.findById(7L)).thenReturn(Optional.of(member(7L)));

		assertThat(service.get(7L).fullName()).isEqualTo("Ada Lovelace");
	}

	@Test
	void getMissingMemberThrowsNotFound() {
		when(repository.findById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(7L)).isInstanceOf(NotFoundException.class)
			.hasMessage("Member 7 not found");
	}

	@Test
	void createAssignsCodeJoinDateAndNormalisedEmail() {
		when(repository.existsByEmail("ada@example.com")).thenReturn(false);
		when(repository.nextMemberCodeNumber()).thenReturn(1L);
		when(repository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

		MemberResponse created = service.create(new MemberRequest("Ada Lovelace", "Ada@Example.COM", "555-0100", null));

		ArgumentCaptor<Member> saved = ArgumentCaptor.forClass(Member.class);
		verify(repository).save(saved.capture());
		assertThat(saved.getValue().getEmail()).isEqualTo("ada@example.com");
		assertThat(created.memberCode()).isEqualTo("M0001");
		assertThat(created.joinedAt()).isEqualTo(NOW);
		assertThat(created.active()).isTrue();
		assertThat(created.phone()).isEqualTo("555-0100");
	}

	@Test
	void createStoresEmptyPhoneAsNone() {
		when(repository.nextMemberCodeNumber()).thenReturn(2L);
		when(repository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

		assertThat(service.create(new MemberRequest("Ada Lovelace", "ada@example.com", "", null)).phone()).isNull();
	}

	@Test
	void createRejectsDuplicateEmailIgnoringCase() {
		when(repository.existsByEmail("ada@example.com")).thenReturn(true);

		assertThatThrownBy(() -> service.create(new MemberRequest("Ada Two", "ADA@example.com", null, null)))
			.isInstanceOf(BusinessRuleException.class)
			.extracting("code")
			.isEqualTo("DUPLICATE");
		verify(repository, never()).nextMemberCodeNumber();
		verify(repository, never()).save(any());
	}

	@Test
	void updateChangesDetailsAndKeepsCode() {
		Member member = member(7L);
		when(repository.findById(7L)).thenReturn(Optional.of(member));
		when(repository.existsByEmailAndIdNot("ada.king@example.com", 7L)).thenReturn(false);
		when(repository.saveAndFlush(member)).thenReturn(member);

		MemberResponse updated = service.update(7L,
				new MemberRequest("Ada King", "Ada.King@example.com", null, VERSION));

		assertThat(updated.fullName()).isEqualTo("Ada King");
		assertThat(updated.email()).isEqualTo("ada.king@example.com");
		assertThat(updated.phone()).isNull();
		assertThat(updated.memberCode()).isEqualTo("M0007");
	}

	@Test
	void updateReturnsVersionAfterFlush() {
		Member member = member(7L);
		when(repository.findById(7L)).thenReturn(Optional.of(member));
		when(repository.saveAndFlush(member)).thenAnswer(inv -> {
			ReflectionTestUtils.setField(member, "version", VERSION + 1);
			return member;
		});

		assertThat(service.update(7L, new MemberRequest("Ada", "ada@example.com", null, VERSION)).version())
			.isEqualTo(VERSION + 1);
	}

	@Test
	void updateWithStaleVersionIsRejectedWithoutChanges() {
		Member member = member(7L);
		when(repository.findById(7L)).thenReturn(Optional.of(member));

		assertThatThrownBy(() -> service.update(7L, new MemberRequest("Ada King", "ada@example.com", null, VERSION - 1)))
			.isInstanceOf(ObjectOptimisticLockingFailureException.class);
		assertThat(member.getFullName()).isEqualTo("Ada Lovelace");
		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void updateRejectsEmailUsedByAnotherMember() {
		when(repository.findById(7L)).thenReturn(Optional.of(member(7L)));
		when(repository.existsByEmailAndIdNot("grace@example.com", 7L)).thenReturn(true);

		assertThatThrownBy(() -> service.update(7L, new MemberRequest("Ada", "grace@example.com", null, VERSION)))
			.isInstanceOf(BusinessRuleException.class)
			.extracting("code")
			.isEqualTo("DUPLICATE");
		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void updateMissingMemberThrowsNotFound() {
		when(repository.findById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(7L, new MemberRequest("Ada", "ada@example.com", null, VERSION)))
			.isInstanceOf(NotFoundException.class);
	}

	@Test
	void deactivateMarksMemberInactive() {
		Member member = member(7L);
		when(repository.findById(7L)).thenReturn(Optional.of(member));
		when(repository.saveAndFlush(member)).thenReturn(member);

		assertThat(service.deactivate(7L).active()).isFalse();
	}

	@Test
	void deactivateMissingMemberThrowsNotFound() {
		when(repository.findById(anyLong())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.deactivate(7L)).isInstanceOf(NotFoundException.class)
			.hasMessage("Member 7 not found");
	}

	private static Member member(Long id) {
		Member member = new Member(Member.formatCode(id), "Ada Lovelace", "ada@example.com", "555-0100", NOW);
		ReflectionTestUtils.setField(member, "id", id);
		ReflectionTestUtils.setField(member, "version", VERSION);
		return member;
	}
}
