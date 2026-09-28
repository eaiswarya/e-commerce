package com.library.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void loginWithSeededAdminReturnsTokenThatAuthorisesMe() throws Exception {
		String body = mockMvc.perform(login("admin", "admin123"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token").isString())
			.andExpect(jsonPath("$.expiresAt").isString())
			.andExpect(jsonPath("$.fullName").value("Administrator"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String token = JsonPath.read(body, "$.token");

		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value("admin"))
			.andExpect(jsonPath("$.fullName").value("Administrator"));
	}

	@Test
	void wrongPasswordReturns401() throws Exception {
		mockMvc.perform(login("admin", "wrong"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
			.andExpect(jsonPath("$.message").value("Invalid username or password"));
	}

	@Test
	void blankCredentialsReturn400() throws Exception {
		mockMvc.perform(login("", ""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.fieldErrors.username").exists())
			.andExpect(jsonPath("$.fieldErrors.password").exists());
	}

	@Test
	void meWithoutTokenReturns401() throws Exception {
		mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
	}

	private static org.springframework.test.web.servlet.RequestBuilder login(String username, String password) {
		return post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password));
	}
}
