package com.library.auth;

import com.library.common.LibraryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class AdminSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

	private final LibrarianRepository repository;

	private final PasswordEncoder passwordEncoder;

	private final LibraryProperties properties;

	public AdminSeeder(LibrarianRepository repository, PasswordEncoder passwordEncoder, LibraryProperties properties) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
		this.properties = properties;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (repository.count() > 0) {
			return;
		}
		LibraryProperties.Admin admin = properties.admin();
		if (!StringUtils.hasText(admin.password())) {
			log.warn("No librarians exist and ADMIN_PASSWORD is not set; skipping admin seed");
			return;
		}
		repository.save(new Librarian(admin.username(), passwordEncoder.encode(admin.password()), admin.fullName()));
		log.info("Seeded admin librarian '{}'", admin.username());
	}
}
