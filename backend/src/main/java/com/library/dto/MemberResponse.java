package com.library.dto;

import com.library.entity.Member;
import java.time.Instant;

/** {@code version} must be sent back in the next update of this member. */
public record MemberResponse(Long id, String memberCode, String fullName, String email, String phone, boolean active,
		Instant joinedAt, Long version) {

	public static MemberResponse from(Member member) {
		return new MemberResponse(member.getId(), member.getMemberCode(), member.getFullName(), member.getEmail(),
				member.getPhone(), member.isActive(), member.getJoinedAt(), member.getVersion());
	}
}
