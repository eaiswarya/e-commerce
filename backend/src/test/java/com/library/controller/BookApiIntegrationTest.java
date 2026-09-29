package com.library.controller;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class BookApiIntegrationTest {

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
	void librarianCanCreateSearchUpdateAndDeleteBooks() throws Exception {
		String created = mockMvc
			.perform(authed(post("/api/books")).content(book("978-1-4028-9462-6", "Zephyr Gardens", "Programming", 3)))
			.andExpect(status().isCreated())
			.andExpect(header().exists(HttpHeaders.LOCATION))
			.andExpect(jsonPath("$.isbn").value("9781402894626"))
			.andExpect(jsonPath("$.availableCopies").value(3))
			.andExpect(jsonPath("$.version").value(0))
			.andReturn()
			.getResponse()
			.getContentAsString();
		long id = ((Number) JsonPath.read(created, "$.id")).longValue();
		long version = ((Number) JsonPath.read(created, "$.version")).longValue();
		long other = ((Number) JsonPath.read(mockMvc
			.perform(authed(post("/api/books")).content(book("0-306-40615-2", "Zephyr Nights", "Fiction", 1)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString(), "$.id")).longValue();

		mockMvc.perform(authed(get("/api/books")).param("q", "zephyr"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(2))
			.andExpect(jsonPath("$.content[*].title", contains("Zephyr Gardens", "Zephyr Nights")));
		mockMvc.perform(authed(get("/api/books")).param("q", "zephyr").param("category", "fiction"))
			.andExpect(jsonPath("$.content[*].title", contains("Zephyr Nights")));
		mockMvc.perform(authed(get("/api/books")).param("q", "9781402894626"))
			.andExpect(jsonPath("$.content[*].title", contains("Zephyr Gardens")));

		mockMvc.perform(authed(post("/api/books")).content(book("9781402894626", "Copy", null, 1)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error").value("DUPLICATE"));

		String zeroCopies = book("9781402894626", "Zephyr Gardens", "Programming", 0);
		mockMvc.perform(authed(put("/api/books/" + id)).content(zeroCopies))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.version").exists());
		mockMvc.perform(authed(put("/api/books/" + id)).content(withVersion(zeroCopies, version)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalCopies").value(0))
			.andExpect(jsonPath("$.availableCopies").value(0))
			.andExpect(jsonPath("$.version").value(version + 1));
		mockMvc.perform(authed(put("/api/books/" + id)).content(withVersion(zeroCopies, version)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error").value("CONCURRENT_UPDATE"));
		mockMvc.perform(authed(get("/api/books/" + id))).andExpect(jsonPath("$.version").value(version + 1));
		mockMvc.perform(authed(get("/api/books")).param("q", "zephyr").param("available", "true"))
			.andExpect(jsonPath("$.content[*].title", contains("Zephyr Nights")));

		mockMvc.perform(authed(delete("/api/books/" + id))).andExpect(status().isNoContent());
		mockMvc.perform(authed(delete("/api/books/" + other))).andExpect(status().isNoContent());
		mockMvc.perform(authed(get("/api/books/" + id)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("NOT_FOUND"));
	}

	@Test
	void booksRequireAToken() throws Exception {
		mockMvc.perform(get("/api/books"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	@Test
	void unknownSortPropertyReturns400() throws Exception {
		mockMvc.perform(authed(get("/api/books")).param("sort", "nope"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("BAD_REQUEST"))
			.andExpect(jsonPath("$.message").value("Unknown sort property 'nope'"));
	}

	@Test
	void pageSizeIsCappedAt100() throws Exception {
		mockMvc.perform(authed(get("/api/books")).param("size", "1000"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.size").value(100));
	}

	private MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer).contentType(MediaType.APPLICATION_JSON);
	}

	private static String book(String isbn, String title, String category, int copies) {
		String categoryJson = (category == null) ? "null" : "\"" + category + "\"";
		return """
				{"isbn":"%s","title":"%s","author":"Test Author","category":%s,"publishedYear":2020,"totalCopies":%d}"""
			.formatted(isbn, title, categoryJson, copies);
	}

	private static String withVersion(String body, long version) {
		return body.substring(0, body.length() - 1) + ",\"version\":" + version + "}";
	}
}
