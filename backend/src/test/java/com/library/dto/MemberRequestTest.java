package com.library.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.groups.Default;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MemberRequestTest {

	private static ValidatorFactory factory;
	private static Validator validator;

	@BeforeAll
	static void setUp() {
		factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@AfterAll
	static void tearDown() {
		factory.close();
	}

	@Test
	void acceptsValidRequest() {
		assertThat(violations(new MemberRequest("Ada Lovelace", "ada@example.com", "+44 (20) 7946-0000", null))).isEmpty();
	}

	@Test
	void acceptsMissingOrEmptyPhone() {
		assertThat(violations(new MemberRequest("Ada Lovelace", "ada@example.com", null, null))).isEmpty();
		assertThat(violations(new MemberRequest("Ada Lovelace", "ada@example.com", "", null))).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "12", "call me", "555-0100 ext. 4", "++44 20", "1234567890123456789012345678901" })
	void rejectsMalformedPhone(String phone) {
		assertThat(violations(new MemberRequest("Ada Lovelace", "ada@example.com", phone, null))).containsExactly("phone");
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "not-an-email", "ada@", " ada@example.com " })
	void rejectsMissingOrMalformedEmail(String email) {
		assertThat(violations(new MemberRequest("Ada Lovelace", email, null, null))).containsExactly("email");
	}

	@Test
	void rejectsBlankOrOverlongName() {
		assertThat(violations(new MemberRequest(" ", "ada@example.com", null, null))).containsExactly("fullName");
		assertThat(violations(new MemberRequest("x".repeat(151), "ada@example.com", null, null)))
			.containsExactly("fullName");
	}

	@Test
	void versionIsOptionalOnCreateButRequiredOnUpdate() {
		MemberRequest request = new MemberRequest("Ada Lovelace", "ada@example.com", null, null);

		assertThat(violations(request)).isEmpty();
		assertThat(violations(request, Default.class, MemberRequest.OnUpdate.class)).containsExactly("version");
	}

	private static Set<String> violations(MemberRequest request, Class<?>... groups) {
		Set<ConstraintViolation<MemberRequest>> result = validator.validate(request, groups);
		return result.stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
	}
}
