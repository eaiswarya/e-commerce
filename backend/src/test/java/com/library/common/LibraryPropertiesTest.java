package com.library.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LibraryPropertiesTest {

	@Autowired
	private LibraryProperties properties;

	@Test
	void bindsLoanDefaultsFromConfig() {
		assertThat(properties.loan().periodDays()).isEqualTo(14);
		assertThat(properties.loan().maxActive()).isEqualTo(5);
	}
}
