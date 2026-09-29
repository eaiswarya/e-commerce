package com.library.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.ThrowingController.class)
@Import({ GlobalExceptionHandlerTest.ThrowingController.class, GlobalExceptionHandler.class })
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void notFoundReturns404() throws Exception {
		mockMvc.perform(get("/test/not-found"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.error").value("NOT_FOUND"))
			.andExpect(jsonPath("$.message").value("Book 7 not found"))
			.andExpect(jsonPath("$.timestamp").isString());
	}

	@Test
	void businessRuleReturns409WithCode() throws Exception {
		mockMvc.perform(get("/test/rule"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error").value("NO_COPIES_AVAILABLE"))
			.andExpect(jsonPath("$.message").value("No copies available"));
	}

	@Test
	void optimisticLockReturns409ConcurrentUpdate() throws Exception {
		mockMvc.perform(get("/test/lock"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error").value("CONCURRENT_UPDATE"));
	}

	@Test
	void validationFailureReturns400WithFieldErrors() throws Exception {
		mockMvc.perform(post("/test/validate").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.fieldErrors.title").exists());
	}

	@Test
	void malformedJsonReturns400() throws Exception {
		mockMvc.perform(post("/test/validate").contentType(MediaType.APPLICATION_JSON).content("{not json"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
	}

	@Test
	void unexpectedErrorReturns500WithoutLeakingDetails() throws Exception {
		mockMvc.perform(get("/test/boom"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.error").value("INTERNAL_ERROR"))
			.andExpect(jsonPath("$.message").value("Unexpected error"));
	}

	@Test
	void unknownRouteReturns404() throws Exception {
		mockMvc.perform(get("/test/nope"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.error").value("NOT_FOUND"));
	}

	@Test
	void wrongMethodReturns405() throws Exception {
		mockMvc.perform(post("/test/not-found"))
			.andExpect(status().isMethodNotAllowed())
			.andExpect(jsonPath("$.status").value(405))
			.andExpect(jsonPath("$.error").value("METHOD_NOT_ALLOWED"));
	}

	@Test
	void typeMismatchReturns400() throws Exception {
		mockMvc.perform(get("/test/typed").param("id", "abc"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("BAD_REQUEST"));
	}

	@Test
	void missingParamReturns400() throws Exception {
		mockMvc.perform(get("/test/typed"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("BAD_REQUEST"));
	}

	@Test
	void authenticationFailureReturns401() throws Exception {
		mockMvc.perform(get("/test/unauthenticated"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	@Test
	void accessDeniedReturns403() throws Exception {
		mockMvc.perform(get("/test/forbidden"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.error").value("FORBIDDEN"));
	}

	@RestController
	static class ThrowingController {

		record Payload(@NotBlank String title) {
		}

		@GetMapping("/test/not-found")
		void notFound() {
			throw new NotFoundException("Book 7 not found");
		}

		@GetMapping("/test/rule")
		void rule() {
			throw new BusinessRuleException("NO_COPIES_AVAILABLE", "No copies available");
		}

		@GetMapping("/test/lock")
		void lock() {
			throw new ObjectOptimisticLockingFailureException(Object.class, 1L);
		}

		@PostMapping("/test/validate")
		void validate(@Valid @RequestBody Payload payload) {
		}

		@GetMapping("/test/typed")
		void typed(@RequestParam long id) {
		}

		@GetMapping("/test/boom")
		void boom() {
			throw new IllegalStateException("secret internals");
		}

		@GetMapping("/test/unauthenticated")
		void unauthenticated() {
			throw new BadCredentialsException("Invalid username or password");
		}

		@GetMapping("/test/forbidden")
		void forbidden() {
			throw new AccessDeniedException("nope");
		}
	}
}
