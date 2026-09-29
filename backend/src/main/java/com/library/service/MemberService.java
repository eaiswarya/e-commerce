package com.library.service;

import static com.library.repository.MemberSpecifications.hasActive;
import static com.library.repository.MemberSpecifications.matchesQuery;

import com.library.dto.MemberRequest;
import com.library.dto.MemberResponse;
import com.library.dto.PageResponse;
import com.library.entity.Member;
import com.library.exception.BusinessRuleException;
import com.library.exception.NotFoundException;
import com.library.repository.MemberRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

	private final MemberRepository repository;

	private final Clock clock;

	public PageResponse<MemberResponse> search(String q, Boolean active, Pageable pageable) {
		Specification<Member> spec = Specification.allOf(matchesQuery(q), hasActive(active));
		return PageResponse.from(repository.findAll(spec, pageable).map(MemberResponse::from));
	}

	public MemberResponse get(Long id) {
		return MemberResponse.from(find(id));
	}

	@Transactional
	public MemberResponse create(MemberRequest request) {
		String email = Member.normaliseEmail(request.email());
		if (repository.existsByEmail(email)) {
			throw duplicateEmail(email);
		}
		String code = Member.formatCode(repository.nextMemberCodeNumber());
		Member member = new Member(code, request.fullName(), email, phone(request), Instant.now(clock));
		return MemberResponse.from(repository.save(member));
	}

	@Transactional
	public MemberResponse update(Long id, MemberRequest request) {
		Member member = find(id);
		if (!member.getVersion().equals(request.version())) {
			// The client edited an out-of-date copy; GlobalExceptionHandler answers 409 CONCURRENT_UPDATE.
			throw new ObjectOptimisticLockingFailureException(Member.class, id);
		}
		String email = Member.normaliseEmail(request.email());
		if (repository.existsByEmailAndIdNot(email, id)) {
			throw duplicateEmail(email);
		}
		member.updateDetails(request.fullName(), email, phone(request));
		// Flush now so the response carries the incremented version the client must send next time.
		return MemberResponse.from(repository.saveAndFlush(member));
	}

	@Transactional
	public MemberResponse deactivate(Long id) {
		Member member = find(id);
		member.deactivate();
		return MemberResponse.from(repository.saveAndFlush(member));
	}

	private Member find(Long id) {
		return repository.findById(id).orElseThrow(() -> new NotFoundException("Member %d not found".formatted(id)));
	}

	private static String phone(MemberRequest request) {
		return StringUtils.hasText(request.phone()) ? request.phone().trim() : null;
	}

	private static BusinessRuleException duplicateEmail(String email) {
		return new BusinessRuleException("DUPLICATE", "A member with email %s already exists".formatted(email));
	}
}
