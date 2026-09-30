package com.library.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Borrows sent at the same moment must never lend more copies than exist, or more loans than a member may hold.
 * Whichever borrow loses gets a 409: {@code CONCURRENT_UPDATE} if it truly overlapped, or the rule's own code if it
 * ran just after the winner.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LoanConcurrencyIntegrationTest {

	private static final int THREADS = 4;

	@Autowired
	private MockMvc mockMvc;

	private String bearer;

	@BeforeEach
	void logIn() throws Exception {
		String body = mockMvc
			.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		bearer = "Bearer " + JsonPath.read(body, "$.token");
	}

	@Test
	void concurrentBorrowsNeverLendMoreCopiesThanExist() throws Exception {
		long book = create("/api/books",
				"{\"isbn\":\"9785000000059\",\"title\":\"Last Copy\",\"author\":\"Test Author\",\"totalCopies\":1}");
		List<Long> members = new ArrayList<>();
		for (int i = 0; i < THREADS; i++) {
			members.add(create("/api/members", "{\"fullName\":\"Racer %d\",\"email\":\"racer%d@example.com\"}".formatted(i, i)));
		}

		List<String> outcomes = borrowAtOnce(members.stream().map(m -> body(book, m)).toList());

		assertThat(outcomes).filteredOn("201"::equals).hasSize(1);
		assertThat(outcomes).filteredOn(o -> !o.equals("201"))
			.hasSize(THREADS - 1)
			.allMatch(o -> o.equals("409 CONCURRENT_UPDATE") || o.equals("409 NO_COPIES_AVAILABLE"));
		assertThat(availableCopies(book)).isZero();
		assertThat(activeLoansOfBook(book)).isEqualTo(1);
	}

	@Test
	void concurrentBorrowsNeverTakeAMemberPastTheLimit() throws Exception {
		long member = create("/api/members", "{\"fullName\":\"Greedy Reader\",\"email\":\"greedy@example.com\"}");
		// Four active loans already: the default limit of 5 leaves room for exactly one more.
		for (int i = 0; i < 4; i++) {
			long warmUp = create("/api/books",
					"{\"isbn\":\"97850000011%02d\",\"title\":\"Warm Up %d\",\"author\":\"A\",\"totalCopies\":1}".formatted(i, i));
			mockMvc.perform(authed(post("/api/loans")).content(body(warmUp, member))).andExpect(status().isCreated());
		}
		List<String> bodies = new ArrayList<>();
		for (int i = 0; i < THREADS; i++) {
			long book = create("/api/books",
					"{\"isbn\":\"97850000012%02d\",\"title\":\"Race %d\",\"author\":\"A\",\"totalCopies\":1}".formatted(i, i));
			bodies.add(body(book, member));
		}

		List<String> outcomes = borrowAtOnce(bodies);

		// The member row lock makes these borrows run one after another, so every loser sees the limit.
		assertThat(outcomes).filteredOn("201"::equals).hasSize(1);
		assertThat(outcomes).filteredOn(o -> !o.equals("201")).hasSize(THREADS - 1).allMatch("409 LOAN_LIMIT_REACHED"::equals);
		String loans = mockMvc.perform(authed(get("/api/members/" + member + "/loans")).param("status", "active"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		assertThat(((Number) JsonPath.read(loans, "$.totalElements")).intValue()).isEqualTo(5);
	}

	/** Releases all borrows together; each outcome is {@code "201"} or the status and error code, e.g. {@code "409 X"}. */
	private List<String> borrowAtOnce(List<String> bodies) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(bodies.size());
		try {
			CountDownLatch start = new CountDownLatch(1);
			List<Future<String>> results = new ArrayList<>();
			for (String body : bodies) {
				Callable<String> borrow = () -> {
					start.await();
					MvcResult result = mockMvc.perform(authed(post("/api/loans")).content(body)).andReturn();
					int status = result.getResponse().getStatus();
					return (status == 201) ? "201"
							: status + " " + JsonPath.read(result.getResponse().getContentAsString(), "$.error");
				};
				results.add(pool.submit(borrow));
			}
			start.countDown();
			List<String> outcomes = new ArrayList<>();
			for (Future<String> result : results) {
				outcomes.add(result.get(30, TimeUnit.SECONDS));
			}
			return outcomes;
		}
		finally {
			pool.shutdownNow();
		}
	}

	private long create(String path, String json) throws Exception {
		String body = mockMvc.perform(authed(post(path)).content(json))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}

	private int availableCopies(long book) throws Exception {
		String body = mockMvc.perform(authed(get("/api/books/" + book))).andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.availableCopies");
	}

	private int activeLoansOfBook(long book) throws Exception {
		String body = mockMvc.perform(authed(get("/api/loans")).param("bookId", String.valueOf(book)).param("status", "active"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		return ((Number) JsonPath.read(body, "$.totalElements")).intValue();
	}

	private MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer).contentType(MediaType.APPLICATION_JSON);
	}

	private static String body(long bookId, long memberId) {
		return "{\"bookId\":%d,\"memberId\":%d}".formatted(bookId, memberId);
	}
}
