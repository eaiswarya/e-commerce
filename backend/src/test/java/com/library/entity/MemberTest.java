package com.library.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class MemberTest {

	private static final Instant JOINED = Instant.parse("2026-09-29T10:00:00Z");

	@Test
	void newMemberIsActive() {
		Member member = new Member("M0001", "Ada Lovelace", "ada@example.com", "+44 20 7946 0000", JOINED);

		assertThat(member.isActive()).isTrue();
		assertThat(member.getMemberCode()).isEqualTo("M0001");
		assertThat(member.getJoinedAt()).isEqualTo(JOINED);
	}

	@Test
	void deactivateMarksMemberInactive() {
		Member member = new Member("M0001", "Ada Lovelace", "ada@example.com", null, JOINED);

		member.deactivate();

		assertThat(member.isActive()).isFalse();
	}

	@Test
	void deactivatingTwiceKeepsMemberInactive() {
		Member member = new Member("M0001", "Ada Lovelace", "ada@example.com", null, JOINED);

		member.deactivate();
		member.deactivate();

		assertThat(member.isActive()).isFalse();
	}

	@Test
	void updateDetailsReplacesNameEmailAndPhone() {
		Member member = new Member("M0001", "Ada Lovelace", "ada@example.com", "123", JOINED);

		member.updateDetails("Ada King", "ada.king@example.com", null);

		assertThat(member.getFullName()).isEqualTo("Ada King");
		assertThat(member.getEmail()).isEqualTo("ada.king@example.com");
		assertThat(member.getPhone()).isNull();
		assertThat(member.getMemberCode()).isEqualTo("M0001");
	}

	@Test
	void normaliseEmailTrimsAndLowercases() {
		assertThat(Member.normaliseEmail("  Ada.Lovelace@Example.COM ")).isEqualTo("ada.lovelace@example.com");
		assertThat(Member.normaliseEmail(null)).isNull();
	}

	@Test
	void formatsMemberCodeFromSequenceNumber() {
		assertThat(Member.formatCode(1)).isEqualTo("M0001");
		assertThat(Member.formatCode(42)).isEqualTo("M0042");
		assertThat(Member.formatCode(12345)).isEqualTo("M12345");
	}
}
