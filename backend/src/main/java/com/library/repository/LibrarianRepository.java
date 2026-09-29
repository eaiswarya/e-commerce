package com.library.repository;

import com.library.entity.Librarian;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibrarianRepository extends JpaRepository<Librarian, Long> {

	Optional<Librarian> findByUsername(String username);
}
