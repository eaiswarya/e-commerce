package com.library.controller;

import com.library.dto.BorrowRequest;
import com.library.dto.LoanResponse;
import com.library.dto.PageResponse;
import com.library.entity.LoanStatus;
import com.library.service.LoanService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

	private final LoanService loanService;

	@GetMapping
	PageResponse<LoanResponse> search(@RequestParam(required = false) LoanStatus status,
			@RequestParam(required = false) Long memberId, @RequestParam(required = false) Long bookId,
			@PageableDefault(size = 20, sort = "borrowedAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return loanService.search(status, memberId, bookId, pageable);
	}

	@GetMapping("/{id}")
	LoanResponse get(@PathVariable Long id) {
		return loanService.get(id);
	}

	@PostMapping
	ResponseEntity<LoanResponse> borrow(@Valid @RequestBody BorrowRequest request) {
		LoanResponse created = loanService.borrow(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(created.id())
			.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PostMapping("/{id}/return")
	LoanResponse returnLoan(@PathVariable Long id) {
		return loanService.returnLoan(id);
	}
}
