package com.library.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.library.dto.BorrowRequest;
import com.library.dto.LoanResponse;
import com.library.dto.PageResponse;
import com.library.entity.Book;
import com.library.entity.LoanStatus;
import com.library.exception.BusinessRuleException;
import com.library.exception.NotFoundException;
import com.library.service.LoanService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LoanController.class)
@AutoConfigureMockMvc(addFilters = false)
class LoanControllerTest {

	private static final LoanResponse LOAN = new LoanResponse(11L, 3L, "Effective Java", "9780134685991", 7L, "M0007",
			"Ada Lovelace", Instant.parse("2026-09-29T10:15:30Z"), LocalDate.parse("2026-10-13"), null,
			LoanStatus.ACTIVE);

	private static final String BODY = "{\"bookId\":3,\"memberId\":7}";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private LoanService service;

	@Test
	void searchDefaultsToNewestFirstWithNoFilters() throws Exception {
		when(service.search(null, null, null, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "borrowedAt"))))
			.thenReturn(new PageResponse<>(List.of(LOAN), 0, 20, 1, 1));

		mockMvc.perform(get("/api/loans"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].memberCode").value("M0007"))
			.andExpect(jsonPath("$.content[0].dueDate").value("2026-10-13"))
			.andExpect(jsonPath("$.content[0].returnedAt").doesNotExist())
			.andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
	}

	@Test
	void searchPassesFiltersAndAcceptsStatusInAnyCase() throws Exception {
		when(service.search(any(), any(), any(), any())).thenReturn(new PageResponse<>(List.of(), 0, 5, 0, 0));

		mockMvc
			.perform(get("/api/loans").param("status", "overdue")
				.param("memberId", "7")
				.param("bookId", "3")
				.param("size", "5")
				.param("sort", "dueDate,asc"))
			.andExpect(status().isOk());

		verify(service).search(LoanStatus.OVERDUE, 7L, 3L, PageRequest.of(0, 5, Sort.by("dueDate")));
	}

	@Test
	void unknownStatusReturns400() throws Exception {
		mockMvc.perform(get("/api/loans").param("status", "lost"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("BAD_REQUEST"));
		verifyNoInteractions(service);
	}

	@Test
	void getReturnsLoan() throws Exception {
		when(service.get(11L)).thenReturn(LOAN);

		mockMvc.perform(get("/api/loans/11")).andExpect(status().isOk()).andExpect(jsonPath("$.bookId").value(3));
	}

	@Test
	void getMissingLoanReturns404() throws Exception {
		when(service.get(11L)).thenThrow(new NotFoundException("Loan 11 not found"));

		mockMvc.perform(get("/api/loans/11"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Loan 11 not found"));
	}

	@Test
	void borrowReturns201WithLocation() throws Exception {
		when(service.borrow(new BorrowRequest(3L, 7L))).thenReturn(LOAN);

		mockMvc.perform(post("/api/loans").contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "http://localhost/api/loans/11"))
			.andExpect(jsonPath("$.id").value(11));
	}

	@Test
	void borrowWithMissingIdsReturns400WithFieldErrors() throws Exception {
		mockMvc.perform(post("/api/loans").contentType(MediaType.APPLICATION_JSON).content("{\"bookId\":0}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.fieldErrors.bookId").exists())
			.andExpect(jsonPath("$.fieldErrors.memberId").exists());
		verifyNoInteractions(service);
	}

	@ParameterizedTest
	@ValueSource(strings = { "NO_COPIES_AVAILABLE", "LOAN_LIMIT_REACHED", "MEMBER_HAS_OVERDUE", "MEMBER_INACTIVE" })
	void brokenBorrowRuleReturns409WithItsCode(String code) throws Exception {
		when(service.borrow(any(BorrowRequest.class))).thenThrow(new BusinessRuleException(code, "Rule broken"));

		mockMvc.perform(post("/api/loans").contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error").value(code));
	}

	@Test
	void borrowRacingForTheLastCopyReturns409() throws Exception {
		when(service.borrow(any(BorrowRequest.class)))
			.thenThrow(new ObjectOptimisticLockingFailureException(Book.class, 3L));

		mockMvc.perform(post("/api/loans").contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error").value("CONCURRENT_UPDATE"));
	}

	@Test
	void returnReturnsTheReturnedLoan() throws Exception {
		LoanResponse returned = new LoanResponse(11L, 3L, "Effective Java", "9780134685991", 7L, "M0007",
				"Ada Lovelace", LOAN.borrowedAt(), LOAN.dueDate(), Instant.parse("2026-10-01T09:00:00Z"),
				LoanStatus.RETURNED);
		when(service.returnLoan(11L)).thenReturn(returned);

		mockMvc.perform(post("/api/loans/11/return"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("RETURNED"))
			.andExpect(jsonPath("$.returnedAt").value("2026-10-01T09:00:00Z"));
	}

	@Test
	void secondReturnReturns409() throws Exception {
		when(service.returnLoan(11L))
			.thenThrow(new BusinessRuleException("ALREADY_RETURNED", "Loan 11 was already returned"));

		mockMvc.perform(post("/api/loans/11/return"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error").value("ALREADY_RETURNED"));
	}
}
