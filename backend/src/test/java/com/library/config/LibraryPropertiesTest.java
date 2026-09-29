package com.library.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
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

	@Test
	void bindsJwtAndAdminDefaultsFromConfig() {
		assertThat(properties.jwt().expiry()).isEqualTo(Duration.ofHours(8));
		assertThat(properties.jwt().secret()).hasSizeGreaterThanOrEqualTo(32);
		assertThat(properties.admin().username()).isEqualTo("admin");
		assertThat(properties.admin().fullName()).isEqualTo("Administrator");
	}
}
