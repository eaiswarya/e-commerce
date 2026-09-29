package com.library.controller;

import com.library.dto.BookRequest;
import com.library.dto.BookResponse;
import com.library.dto.PageResponse;
import com.library.service.BookService;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

	private final BookService bookService;

	@GetMapping
	PageResponse<BookResponse> search(@RequestParam(required = false) String q,
			@RequestParam(required = false) String category, @RequestParam(required = false) Boolean available,
			@PageableDefault(size = 20, sort = "title") Pageable pageable) {
		return bookService.search(q, category, available, pageable);
	}

	@GetMapping("/{id}")
	BookResponse get(@PathVariable Long id) {
		return bookService.get(id);
	}

	@PostMapping
	ResponseEntity<BookResponse> create(@Valid @RequestBody BookRequest request) {
		BookResponse created = bookService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(created.id())
			.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{id}")
	BookResponse update(@PathVariable Long id,
			@Validated({ Default.class, BookRequest.OnUpdate.class }) @RequestBody BookRequest request) {
		return bookService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable Long id) {
		bookService.delete(id);
	}
}
