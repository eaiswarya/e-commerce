package com.library.controller;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class LoanApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	private String bearer;

	@BeforeEach
	void logIn() throws Exception {
		String body = mockMvc
			.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		bearer = "Bearer " + JsonPath.read(body, "$.token");
	}

	@Test
	void librarianCanLendAndTakeBackABook() throws Exception {
		long book = id(createBook("9785000000011", "Loan Flow Book", 1));
		long member = id(createMember("Lena Flow", "lena.flow@example.com"));

		String loan = borrow(book, member).andExpect(status().isCreated())
			.andExpect(header().exists(HttpHeaders.LOCATION))
			.andExpect(jsonPath("$.bookTitle").value("Loan Flow Book"))
			.andExpect(jsonPath("$.memberName").value("Lena Flow"))
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.dueDate").value(LocalDate.now(ZoneOffset.UTC).plusDays(14).toString()))
			.andReturn()
			.getResponse()
			.getContentAsString();
		long loanId = id(loan);
		mockMvc.perform(authed(get("/api/books/" + book))).andExpect(jsonPath("$.availableCopies").value(0));

		// The only copy is out, so nobody else can borrow it.
		long other = id(createMember("Otto Flow", "otto.flow@example.com"));
		borrow(book, other).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("NO_COPIES_AVAILABLE"));
		mockMvc.perform(authed(delete("/api/books/" + book)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error").value("HAS_ACTIVE_LOANS"));
		mockMvc.perform(authed(get("/api/members/" + member + "/loans")).param("status", "active"))
			.andExpect(jsonPath("$.content[*].id").value(contains((int) loanId)));

		mockMvc.perform(authed(post("/api/loans/" + loanId + "/return")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("RETURNED"))
			.andExpect(jsonPath("$.returnedAt").exists());
		mockMvc.perform(authed(get("/api/books/" + book))).andExpect(jsonPath("$.availableCopies").value(1));
		mockMvc.perform(authed(post("/api/loans/" + loanId + "/return")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error").value("ALREADY_RETURNED"));

		mockMvc.perform(authed(get("/api/loans")).param("bookId", String.valueOf(book)).param("status", "returned"))
			.andExpect(jsonPath("$.content[*].id").value(contains((int) loanId)));
		mockMvc.perform(authed(get("/api/members/" + member + "/loans")).param("status", "active"))
			.andExpect(jsonPath("$.totalElements").value(0));

		// Returned loans keep the book in the member's history, so it can no longer be deleted.
		mockMvc.perform(authed(delete("/api/books/" + book)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error").value("HAS_LOAN_HISTORY"));
		mockMvc.perform(authed(get("/api/members/" + member + "/loans")))
			.andExpect(jsonPath("$.content[*].id").value(contains((int) loanId)));
	}

	@Test
	void memberCannotBorrowMoreThanTheLimit() throws Exception {
		long book = id(createBook("9785000000028", "Popular Book", 10));
		long member = id(createMember("Max Limit", "max.limit@example.com"));
		for (int i = 0; i < 5; i++) {
			borrow(book, member).andExpect(status().isCreated());
		}

		borrow(book, member).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("LOAN_LIMIT_REACHED"));
		mockMvc.perform(authed(get("/api/books/" + book))).andExpect(jsonPath("$.availableCopies").value(5));
	}

	@Test
	void inactiveMemberCannotBorrow() throws Exception {
		long book = id(createBook("9785000000035", "Quiet Book", 1));
		long member = id(createMember("Ina Active", "ina.active@example.com"));
		mockMvc.perform(authed(patch("/api/members/" + member + "/deactivate"))).andExpect(status().isOk());

		borrow(book, member).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("MEMBER_INACTIVE"));
	}

	@Test
	void bookThatWasNeverBorrowedCanStillBeDeleted() throws Exception {
		long book = id(createBook("9785000000042", "Unread Book", 1));

		mockMvc.perform(authed(delete("/api/books/" + book))).andExpect(status().isNoContent());
	}

	@Test
	void borrowingUnknownBookOrMemberReturns404() throws Exception {
		long member = id(createMember("Nora Found", "nora.found@example.com"));

		borrow(999_999L, member).andExpect(status().isNotFound());
		mockMvc.perform(authed(get("/api/members/999999/loans"))).andExpect(status().isNotFound());
		mockMvc.perform(authed(post("/api/loans/999999/return"))).andExpect(status().isNotFound());
	}

	@Test
	void requestWithoutTokenReturns401() throws Exception {
		mockMvc.perform(get("/api/loans")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/loans").contentType(MediaType.APPLICATION_JSON).content("{\"bookId\":1,\"memberId\":1}"))
			.andExpect(status().isUnauthorized());
	}

	private String createBook(String isbn, String title, int copies) throws Exception {
		return mockMvc
			.perform(authed(post("/api/books"))
				.content("{\"isbn\":\"%s\",\"title\":\"%s\",\"author\":\"Test Author\",\"totalCopies\":%d}"
					.formatted(isbn, title, copies)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
	}

	private String createMember(String fullName, String email) throws Exception {
		return mockMvc
			.perform(authed(post("/api/members")).content("{\"fullName\":\"%s\",\"email\":\"%s\"}".formatted(fullName, email)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
	}

	private ResultActions borrow(long bookId, long memberId) throws Exception {
		return mockMvc.perform(
				authed(post("/api/loans")).content("{\"bookId\":%d,\"memberId\":%d}".formatted(bookId, memberId)));
	}

	private MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer).contentType(MediaType.APPLICATION_JSON);
	}

	private static long id(String json) {
		return ((Number) JsonPath.read(json, "$.id")).longValue();
	}
}
