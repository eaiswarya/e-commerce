package com.library.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.library.dto.BookRequest;
import com.library.dto.BookResponse;
import com.library.dto.PageResponse;
import com.library.exception.BusinessRuleException;
import com.library.exception.NotFoundException;
import com.library.service.BookService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookController.class)
@AutoConfigureMockMvc(addFilters = false)
class BookControllerTest {

	private static final BookResponse BOOK = new BookResponse(7L, "9780134685991", "Effective Java", "Joshua Bloch",
			"Programming", 2018, 3, 2);

	private static final String BODY = """
			{"isbn":"978-0-13-468599-1","title":"Effective Java","author":"Joshua Bloch",
			 "category":"Programming","publishedYear":2018,"totalCopies":3}""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private BookService service;

	@Test
	void searchUsesDefaultPagingSortedByTitle() throws Exception {
		when(service.search(null, null, null, PageRequest.of(0, 20, Sort.by("title"))))
			.thenReturn(new PageResponse<>(List.of(BOOK), 0, 20, 1, 1));

		mockMvc.perform(get("/api/books"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].title").value("Effective Java"))
			.andExpect(jsonPath("$.content[0].availableCopies").value(2))
			.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void searchPassesFiltersAndPaging() throws Exception {
		when(service.search(any(), any(), any(), any())).thenReturn(new PageResponse<>(List.of(), 2, 5, 0, 0));

		mockMvc
			.perform(get("/api/books").param("q", "java")
				.param("category", "Programming")
				.param("available", "true")
				.param("page", "2")
				.param("size", "5")
				.param("sort", "author,desc"))
			.andExpect(status().isOk());

		verify(service).search("java", "Programming", true, PageRequest.of(2, 5, Sort.by(Sort.Direction.DESC, "author")));
	}

	@Test
	void getReturnsBook() throws Exception {
		when(service.get(7L)).thenReturn(BOOK);

		mockMvc.perform(get("/api/books/7")).andExpect(status().isOk()).andExpect(jsonPath("$.isbn").value("9780134685991"));
	}

	@Test
	void getMissingBookReturns404() throws Exception {
		when(service.get(7L)).thenThrow(new NotFoundException("Book 7 not found"));

		mockMvc.perform(get("/api/books/7"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("NOT_FOUND"))
			.andExpect(jsonPath("$.message").value("Book 7 not found"));
	}

	@Test
	void nonNumericIdReturns400() throws Exception {
		mockMvc.perform(get("/api/books/abc")).andExpect(status().isBadRequest());
		verifyNoInteractions(service);
	}

	@Test
	void createReturns201WithLocation() throws Exception {
		when(service.create(any(BookRequest.class))).thenReturn(BOOK);

		mockMvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "http://localhost/api/books/7"))
			.andExpect(jsonPath("$.id").value(7));

		verify(service).create(new BookRequest("978-0-13-468599-1", "Effective Java", "Joshua Bloch", "Programming",
				2018, 3));
	}

	@Test
	void createWithInvalidBodyReturns400WithFieldErrors() throws Exception {
		mockMvc
			.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
				.content("{\"isbn\":\"123\",\"title\":\"\",\"author\":\"A\",\"totalCopies\":-1}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.fieldErrors.isbn").exists())
			.andExpect(jsonPath("$.fieldErrors.title").exists())
			.andExpect(jsonPath("$.fieldErrors.totalCopies").exists());
		verifyNoInteractions(service);
	}

	@Test
	void createDuplicateReturns409() throws Exception {
		when(service.create(any(BookRequest.class)))
			.thenThrow(new BusinessRuleException("DUPLICATE", "A book with ISBN 9780134685991 already exists"));

		mockMvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error").value("DUPLICATE"));
	}

	@Test
	void updateReturnsUpdatedBook() throws Exception {
		when(service.update(eq(7L), any(BookRequest.class))).thenReturn(BOOK);

		mockMvc.perform(put("/api/books/7").contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(7));
	}

	@Test
	void updateBelowCopiesOnLoanReturns409() throws Exception {
		when(service.update(eq(7L), any(BookRequest.class)))
			.thenThrow(new BusinessRuleException("COPIES_ON_LOAN", "Cannot set total copies to 0: 1 copies are on loan"));

		mockMvc.perform(put("/api/books/7").contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error").value("COPIES_ON_LOAN"));
	}

	@Test
	void deleteReturns204() throws Exception {
		mockMvc.perform(delete("/api/books/7")).andExpect(status().isNoContent());

		verify(service).delete(7L);
	}

	@Test
	void deleteWithCopiesOnLoanReturns409() throws Exception {
		doThrow(new BusinessRuleException("HAS_ACTIVE_LOANS", "Book 7 has 1 copies on loan")).when(service).delete(7L);

		mockMvc.perform(delete("/api/books/7"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error").value("HAS_ACTIVE_LOANS"));
	}
}
