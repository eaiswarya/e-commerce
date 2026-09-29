package com.library.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Create/update payload. The ISBN may contain single hyphens or spaces between digits; the service strips them. */
public record BookRequest(
		@NotBlank @Pattern(regexp = ISBN, message = "must be a 10- or 13-digit ISBN") String isbn,
		@NotBlank @Size(max = 200) String title,
		@NotBlank @Size(max = 150) String author,
		@Size(max = 50) String category,
		@Min(1450) @Max(2100) Integer publishedYear,
		@NotNull @Min(0) @Max(1000) Integer totalCopies) {

	/** ISBN-10 (last character may be X) or ISBN-13, with optional single separators between characters. */
	static final String ISBN = "^(?:\\d[- ]?){9}[\\dXx]$|^(?:\\d[- ]?){12}\\d$";
}
