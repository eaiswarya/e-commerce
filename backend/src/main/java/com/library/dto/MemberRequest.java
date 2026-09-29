package com.library.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Create/update payload. The email is stored lowercased; an empty phone is stored as none.
 * {@code version} is ignored on create and required on update (validation group {@link OnUpdate}): it must be the
 * version the client last read, so an edit based on stale data is rejected.
 */
public record MemberRequest(
		@NotBlank @Size(max = 150) String fullName,
		@NotBlank @Email @Size(max = 254) String email,
		@Pattern(regexp = PHONE, message = "must be a phone number") String phone,
		@NotNull(groups = OnUpdate.class) Long version) {

	/** Empty, or an optional leading {@code +} then 3–29 digits, spaces, hyphens or parentheses, with at least one digit. */
	static final String PHONE = "^$|^\\+?(?=[^0-9]*[0-9])[0-9 ()-]{3,29}$";

	/** Validation group for constraints that apply only when updating. */
	public interface OnUpdate {
	}
}
