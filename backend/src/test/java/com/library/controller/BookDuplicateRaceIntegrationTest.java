package com.library.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.library.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Two requests creating the same ISBN at once can both pass the service's duplicate check; the database's unique
 * index then rejects the second insert. The spy makes the check miss, so the insert itself must produce the 409.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookDuplicateRaceIntegrationTest {

	private static final String BODY = """
			{"isbn":"9783161484100","title":"Race Book","author":"Test Author","totalCopies":1}""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoSpyBean
	private BookRepository repository;

	@Test
	void duplicateIsbnCaughtByDatabaseReturns409() throws Exception {
		String bearer = "Bearer " + JsonPath.read(mockMvc
			.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
			.andReturn()
			.getResponse()
			.getContentAsString(), "$.token");
		doReturn(false).when(repository).existsByIsbn(anyString());

		String created = mockMvc
			.perform(post("/api/books").header(HttpHeaders.AUTHORIZATION, bearer)
				.contentType(MediaType.APPLICATION_JSON)
				.content(BODY))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();

		mockMvc
			.perform(post("/api/books").header(HttpHeaders.AUTHORIZATION, bearer)
				.contentType(MediaType.APPLICATION_JSON)
				.content(BODY))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error").value("DUPLICATE"))
			.andExpect(jsonPath("$.message").value("A record with the same unique value already exists"));

		mockMvc.perform(delete("/api/books/" + JsonPath.read(created, "$.id")).header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(status().isNoContent());
	}
}
