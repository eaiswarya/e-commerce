package com.library.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A library member. Members are deactivated, never deleted, so their loan history is kept. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "member_code", nullable = false, unique = true, length = 10, updatable = false)
	private String memberCode;

	@Column(name = "full_name", nullable = false, length = 150)
	private String fullName;

	@Column(nullable = false, unique = true, length = 254)
	private String email;

	@Column(length = 30)
	private String phone;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "joined_at", nullable = false, updatable = false)
	private Instant joinedAt;

	@Version
	@Column(nullable = false)
	private Long version;

	public Member(String memberCode, String fullName, String email, String phone, Instant joinedAt) {
		this.memberCode = memberCode;
		this.fullName = fullName;
		this.email = email;
		this.phone = phone;
		this.joinedAt = joinedAt;
		this.active = true;
	}

	/** Member codes are {@code M} plus the sequence number, zero-padded to four digits: {@code M0001}. */
	public static String formatCode(long number) {
		return "M%04d".formatted(number);
	}

	/** Trims and lowercases, so the unique email index also rejects addresses that differ only in case. */
	public static String normaliseEmail(String email) {
		return (email == null) ? null : email.trim().toLowerCase(Locale.ROOT);
	}

	public void updateDetails(String fullName, String email, String phone) {
		this.fullName = fullName;
		this.email = email;
		this.phone = phone;
	}

	/** Idempotent: deactivating an inactive member changes nothing. */
	public void deactivate() {
		this.active = false;
	}
}
