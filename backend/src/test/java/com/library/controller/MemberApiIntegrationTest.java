package com.library.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class MemberApiIntegrationTest {

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
	void librarianCanCreateSearchUpdateAndDeactivateMembers() throws Exception {
		String created = mockMvc
			.perform(authed(post("/api/members")).content(member("Quentin Zephyrine", "Quentin.Z@Example.com")))
			.andExpect(status().isCreated())
			.andExpect(header().exists(HttpHeaders.LOCATION))
			.andExpect(jsonPath("$.memberCode").value(matchesPattern("M\\d{4,}")))
			.andExpect(jsonPath("$.email").value("quentin.z@example.com"))
			.andExpect(jsonPath("$.active").value(true))
			.andExpect(jsonPath("$.version").value(0))
			.andReturn()
			.getResponse()
			.getContentAsString();
		long id = ((Number) JsonPath.read(created, "$.id")).longValue();
		String code = JsonPath.read(created, "$.memberCode");
		mockMvc.perform(authed(post("/api/members")).content(member("Rosalind Zephyrine", "rosalind.z@example.com")))
			.andExpect(status().isCreated());

		mockMvc.perform(authed(get("/api/members")).param("q", "zephyrine"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].fullName").value(contains("Quentin Zephyrine", "Rosalind Zephyrine")));
		mockMvc.perform(authed(get("/api/members")).param("q", code.toLowerCase()))
			.andExpect(jsonPath("$.content[*].fullName").value(contains("Quentin Zephyrine")));

		mockMvc.perform(authed(post("/api/members")).content(member("Someone Else", "QUENTIN.Z@example.com")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error").value("DUPLICATE"));

		mockMvc.perform(authed(put("/api/members/" + id)).content(update("Quentin Zephyrine-Ross", 0)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.fullName").value("Quentin Zephyrine-Ross"))
			.andExpect(jsonPath("$.memberCode").value(code))
			.andExpect(jsonPath("$.version").value(1));
		mockMvc.perform(authed(put("/api/members/" + id)).content(update("Stale Edit", 0)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error").value("CONCURRENT_UPDATE"));

		mockMvc.perform(authed(patch("/api/members/" + id + "/deactivate")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.active").value(false));
		mockMvc.perform(authed(patch("/api/members/" + id + "/deactivate")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.active").value(false));

		mockMvc.perform(authed(get("/api/members")).param("active", "false").param("size", "100"))
			.andExpect(jsonPath("$.content[*].fullName").value(hasItem("Quentin Zephyrine-Ross")));
		mockMvc.perform(authed(get("/api/members")).param("q", "zephyrine").param("active", "true"))
			.andExpect(jsonPath("$.content[*].fullName").value(contains("Rosalind Zephyrine")));
		mockMvc.perform(authed(get("/api/members/" + id)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.fullName").value(not("Stale Edit")));
	}

	@Test
	void missingMemberReturns404() throws Exception {
		mockMvc.perform(authed(get("/api/members/999999")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("NOT_FOUND"));
	}

	@Test
	void unknownSortFieldReturns400() throws Exception {
		mockMvc.perform(authed(get("/api/members")).param("sort", "nope"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("BAD_REQUEST"));
	}

	@Test
	void requestWithoutTokenReturns401() throws Exception {
		mockMvc.perform(get("/api/members"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	private MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer).contentType(MediaType.APPLICATION_JSON);
	}

	private static String member(String fullName, String email) {
		return "{\"fullName\":\"%s\",\"email\":\"%s\"}".formatted(fullName, email);
	}

	private static String update(String fullName, long version) {
		return "{\"fullName\":\"%s\",\"email\":\"quentin.z@example.com\",\"version\":%d}".formatted(fullName, version);
	}
}
