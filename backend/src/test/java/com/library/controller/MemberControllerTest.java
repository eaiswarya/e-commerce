package com.library.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.library.dto.LoanResponse;
import com.library.dto.MemberRequest;
import com.library.dto.MemberResponse;
import com.library.dto.PageResponse;
import com.library.entity.LoanStatus;
import com.library.entity.Member;
import com.library.exception.BusinessRuleException;
import com.library.exception.NotFoundException;
import com.library.service.LoanService;
import com.library.service.MemberService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MemberController.class)
@AutoConfigureMockMvc(addFilters = false)
class MemberControllerTest {

	private static final MemberResponse MEMBER = new MemberResponse(7L, "M0007", "Ada Lovelace", "ada@example.com",
			"555-0100", true, Instant.parse("2026-09-29T10:15:30Z"), 4L);

	private static final String BODY = """
			{"fullName":"Ada Lovelace","email":"ada@example.com","phone":"555-0100"}""";

	private static final String UPDATE_BODY = BODY.replace("}", ",\"version\":4}");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MemberService service;

	@MockitoBean
	private LoanService loanService;

	@Test
	void searchUsesDefaultPagingSortedByFullName() throws Exception {
		when(service.search(null, null, PageRequest.of(0, 20, Sort.by("fullName"))))
			.thenReturn(new PageResponse<>(List.of(MEMBER), 0, 20, 1, 1));

		mockMvc.perform(get("/api/members"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].memberCode").value("M0007"))
			.andExpect(jsonPath("$.content[0].active").value(true))
			.andExpect(jsonPath("$.content[0].joinedAt").value("2026-09-29T10:15:30Z"))
			.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void searchPassesFiltersAndPaging() throws Exception {
		when(service.search(any(), any(), any())).thenReturn(new PageResponse<>(List.of(), 1, 5, 0, 0));

		mockMvc
			.perform(get("/api/members").param("q", "ada")
				.param("active", "false")
				.param("page", "1")
				.param("size", "5")
				.param("sort", "memberCode,desc"))
			.andExpect(status().isOk());

		verify(service).search("ada", false, PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "memberCode")));
	}

	@Test
	void invalidActiveFilterReturns400() throws Exception {
		mockMvc.perform(get("/api/members").param("active", "maybe"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("BAD_REQUEST"));
		verifyNoInteractions(service);
	}

	@Test
	void getReturnsMember() throws Exception {
		when(service.get(7L)).thenReturn(MEMBER);

		mockMvc.perform(get("/api/members/7"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("ada@example.com"));
	}

	@Test
	void getMissingMemberReturns404() throws Exception {
		when(service.get(7L)).thenThrow(new NotFoundException("Member 7 not found"));

		mockMvc.perform(get("/api/members/7"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("NOT_FOUND"))
			.andExpect(jsonPath("$.message").value("Member 7 not found"));
	}

	@Test
	void createReturns201WithLocation() throws Exception {
		when(service.create(any(MemberRequest.class))).thenReturn(MEMBER);

		mockMvc.perform(post("/api/members").contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "http://localhost/api/members/7"))
			.andExpect(jsonPath("$.memberCode").value("M0007"))
			.andExpect(jsonPath("$.version").value(4));

		verify(service).create(new MemberRequest("Ada Lovelace", "ada@example.com", "555-0100", null));
	}

	@Test
	void createWithInvalidBodyReturns400WithFieldErrors() throws Exception {
		mockMvc
			.perform(post("/api/members").contentType(MediaType.APPLICATION_JSON)
				.content("{\"fullName\":\"\",\"email\":\"not-an-email\",\"phone\":\"call me\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.fieldErrors.fullName").exists())
			.andExpect(jsonPath("$.fieldErrors.email").exists())
			.andExpect(jsonPath("$.fieldErrors.phone").exists());
		verifyNoInteractions(service);
	}

	@Test
	void createDuplicateEmailReturns409() throws Exception {
		when(service.create(any(MemberRequest.class)))
			.thenThrow(new BusinessRuleException("DUPLICATE", "A member with email ada@example.com already exists"));

		mockMvc.perform(post("/api/members").contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error").value("DUPLICATE"));
	}

	@Test
	void updateReturnsUpdatedMember() throws Exception {
		when(service.update(eq(7L), any(MemberRequest.class))).thenReturn(MEMBER);

		mockMvc.perform(put("/api/members/7").contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(7));

		verify(service).update(7L, new MemberRequest("Ada Lovelace", "ada@example.com", "555-0100", 4L));
	}

	@Test
	void updateWithoutVersionReturns400() throws Exception {
		mockMvc.perform(put("/api/members/7").contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.fieldErrors.version").exists());
		verifyNoInteractions(service);
	}

	@Test
	void updateWithStaleVersionReturns409() throws Exception {
		when(service.update(eq(7L), any(MemberRequest.class)))
			.thenThrow(new ObjectOptimisticLockingFailureException(Member.class, 7L));

		mockMvc.perform(put("/api/members/7").contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error").value("CONCURRENT_UPDATE"));
	}

	@Test
	void deactivateReturnsMember() throws Exception {
		MemberResponse inactive = new MemberResponse(7L, "M0007", "Ada Lovelace", "ada@example.com", "555-0100",
				false, MEMBER.joinedAt(), 5L);
		when(service.deactivate(7L)).thenReturn(inactive);

		mockMvc.perform(patch("/api/members/7/deactivate"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.active").value(false));
	}

	@Test
	void deactivateMissingMemberReturns404() throws Exception {
		when(service.deactivate(7L)).thenThrow(new NotFoundException("Member 7 not found"));

		mockMvc.perform(patch("/api/members/7/deactivate")).andExpect(status().isNotFound());
	}

	@Test
	void membersCannotBeDeleted() throws Exception {
		mockMvc.perform(delete("/api/members/7")).andExpect(status().isMethodNotAllowed());
		verifyNoInteractions(service);
	}

	@Test
	void memberLoansDefaultToAllStatusesNewestFirst() throws Exception {
		LoanResponse loan = new LoanResponse(11L, 3L, "Effective Java", "9780134685991", 7L, "M0007", "Ada Lovelace",
				MEMBER.joinedAt(), LocalDate.parse("2026-10-13"), null, LoanStatus.ACTIVE);
		when(loanService.memberLoans(7L, null, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "borrowedAt"))))
			.thenReturn(new PageResponse<>(List.of(loan), 0, 20, 1, 1));

		mockMvc.perform(get("/api/members/7/loans"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].bookTitle").value("Effective Java"))
			.andExpect(jsonPath("$.content[0].dueDate").value("2026-10-13"))
			.andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
	}

	@Test
	void memberLoansAcceptStatusInAnyCase() throws Exception {
		when(loanService.memberLoans(eq(7L), any(), any())).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

		mockMvc.perform(get("/api/members/7/loans").param("status", "Returned")).andExpect(status().isOk());

		verify(loanService).memberLoans(7L, LoanStatus.RETURNED,
				PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "borrowedAt")));
	}

	@Test
	void memberLoansWithUnknownStatusReturns400() throws Exception {
		mockMvc.perform(get("/api/members/7/loans").param("status", "lost"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("BAD_REQUEST"));
		verifyNoInteractions(loanService);
	}

	@Test
	void loansOfMissingMemberReturn404() throws Exception {
		when(loanService.memberLoans(eq(7L), any(), any())).thenThrow(new NotFoundException("Member 7 not found"));

		mockMvc.perform(get("/api/members/7/loans")).andExpect(status().isNotFound());
	}
}
