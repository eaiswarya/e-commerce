package com.library.controller;

import com.library.dto.MemberRequest;
import com.library.dto.MemberResponse;
import com.library.dto.PageResponse;
import com.library.service.MemberService;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

	private final MemberService memberService;

	@GetMapping
	PageResponse<MemberResponse> search(@RequestParam(required = false) String q,
			@RequestParam(required = false) Boolean active,
			@PageableDefault(size = 20, sort = "fullName") Pageable pageable) {
		return memberService.search(q, active, pageable);
	}

	@GetMapping("/{id}")
	MemberResponse get(@PathVariable Long id) {
		return memberService.get(id);
	}

	@PostMapping
	ResponseEntity<MemberResponse> create(@Valid @RequestBody MemberRequest request) {
		MemberResponse created = memberService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(created.id())
			.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{id}")
	MemberResponse update(@PathVariable Long id,
			@Validated({ Default.class, MemberRequest.OnUpdate.class }) @RequestBody MemberRequest request) {
		return memberService.update(id, request);
	}

	/** Members are never deleted, so their loan history is kept. Repeating the call is harmless. */
	@PatchMapping("/{id}/deactivate")
	MemberResponse deactivate(@PathVariable Long id) {
		return memberService.deactivate(id);
	}
}
