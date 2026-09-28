package com.library.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "library")
public record LibraryProperties(@Valid Loan loan) {

	public record Loan(@Min(1) int periodDays, @Min(1) int maxActive) {
	}
}
