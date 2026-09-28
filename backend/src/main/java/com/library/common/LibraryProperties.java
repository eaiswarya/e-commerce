package com.library.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "library")
public record LibraryProperties(@Valid @NotNull Loan loan, @Valid @NotNull Jwt jwt, @Valid @NotNull Admin admin) {

	public record Loan(@Min(1) int periodDays, @Min(1) int maxActive) {
	}

	/** HS256 needs a key of at least 256 bits. */
	public record Jwt(@NotBlank @Size(min = 32) String secret, @NotNull Duration expiry) {
	}

	/** Seeded on first start when no librarians exist; a blank password skips seeding. */
	public record Admin(@NotBlank String username, String password, @NotBlank String fullName) {
	}
}
