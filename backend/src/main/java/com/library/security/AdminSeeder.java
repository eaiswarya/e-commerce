package com.library.security;

import com.library.config.LibraryProperties;
import com.library.entity.Librarian;
import com.library.repository.LibrarianRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

	private final LibrarianRepository repository;
	private final PasswordEncoder passwordEncoder;
	private final LibraryProperties properties;

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
