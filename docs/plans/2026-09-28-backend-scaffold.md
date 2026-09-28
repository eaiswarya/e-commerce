# Backend Scaffold Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Create the Spring Boot API skeleton in `backend/` with dev/prod profiles, Flyway, library config properties, a health check, a global error handler and a paged-response DTO. This is PR 1 of the design in `docs/plans/2026-09-28-library-management-design.md`.

**Architecture:** Spring Boot 4.1.1 on Java 17, Maven wrapper. Default profile `dev` uses in-memory H2; `prod` uses PostgreSQL from env vars. Flyway owns the schema (no migrations yet — tables arrive with their feature PRs); Hibernate `ddl-auto=validate`. Shared web plumbing lives in `com.library.common`.

**Tech Stack:** Spring Boot 4.1.1 (webmvc, data-jpa, validation, flyway, actuator), H2, PostgreSQL driver, JUnit 5, Spring Boot Test, JaCoCo.

**Not in this PR:** Spring Security (added in PR 2 `feature/auth` — adding it now would lock every endpoint with a generated password).

**Shell:** all commands are Git Bash, run from repo root unless a `cd` is shown. On Windows `./mvnw` works in Git Bash; `mvnw.cmd` in PowerShell.

---

### Task 1: Generate the project

**Files:**
- Create: `backend/` (generated), `backend/src/main/resources/application.yml`
- Delete: `backend/HELP.md`, `backend/src/main/resources/application.properties`

**Step 1: Download and unzip**

```bash
curl -s -o /tmp/boot.zip "https://start.spring.io/starter.zip?type=maven-project&language=java&bootVersion=4.1.1&groupId=com.library&artifactId=library-api&name=library-api&packageName=com.library&javaVersion=17&dependencies=web,data-jpa,validation,flyway,h2,postgresql,actuator"
mkdir backend && unzip -q /tmp/boot.zip -d backend
rm backend/HELP.md backend/src/main/resources/application.properties
```

(Use the session scratchpad instead of `/tmp` when running under Claude Code.)

**Step 2: Rename the main class to `LibraryApplication`**

Rename `backend/src/main/java/com/library/LibraryApiApplication.java` → `LibraryApplication.java`:

```java
package com.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LibraryApplication {

	public static void main(String[] args) {
		SpringApplication.run(LibraryApplication.class, args);
	}
}
```

Rename the test `LibraryApiApplicationTests.java` → `LibraryApplicationTests.java` and its class name to match.

**Step 3: Minimal config so the context starts**

`backend/src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: library-api
```

**Step 4: Run the build**

Run: `cd backend && ./mvnw -q verify`
Expected: BUILD SUCCESS, `LibraryApplicationTests.contextLoads` passes (H2 auto-configured).

**Step 5: Commit**

```bash
git add backend
git commit -m "chore(backend): generate Spring Boot 4.1.1 project"
```

---

### Task 2: Profiles and datasource config

**Files:**
- Modify: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/application-dev.yml`, `backend/src/main/resources/application-prod.yml`, `backend/src/main/resources/db/migration/.gitkeep`

**Step 1: Shared config** — `application.yml`:

```yaml
spring:
  application:
    name: library-api
  profiles:
    default: dev
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
  flyway:
    locations: classpath:db/migration

management:
  endpoints:
    web:
      exposure:
        include: health
```

**Step 2: Dev profile** — `application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:library;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1
    username: sa
    password:
  h2:
    console:
      enabled: true
```

**Step 3: Prod profile** — `application-prod.yml`:

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

**Step 4: Keep the migration folder**

```bash
mkdir -p backend/src/main/resources/db/migration && touch backend/src/main/resources/db/migration/.gitkeep
```

**Step 5: Run the build**

Run: `cd backend && ./mvnw -q verify`
Expected: BUILD SUCCESS (context loads on `dev` profile, Flyway finds no migrations).

**Step 6: Commit**

```bash
git add backend/src/main/resources
git commit -m "chore(backend): add dev (H2) and prod (PostgreSQL) profiles"
```

---

### Task 3: Library config properties

**Files:**
- Create: `backend/src/main/java/com/library/common/LibraryProperties.java`
- Modify: `backend/src/main/java/com/library/LibraryApplication.java`, `backend/src/main/resources/application.yml`
- Test: `backend/src/test/java/com/library/common/LibraryPropertiesTest.java`

**Step 1: Write the failing test**

```java
package com.library.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LibraryPropertiesTest {

	@Autowired
	private LibraryProperties properties;

	@Test
	void bindsLoanDefaultsFromConfig() {
		assertThat(properties.loan().periodDays()).isEqualTo(14);
		assertThat(properties.loan().maxActive()).isEqualTo(5);
	}
}
```

**Step 2: Run it to verify it fails**

Run: `cd backend && ./mvnw -q test -Dtest=LibraryPropertiesTest`
Expected: compilation FAIL — `LibraryProperties` does not exist.

**Step 3: Implement**

`LibraryProperties.java`:

```java
package com.library.common;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "library")
public record LibraryProperties(Loan loan) {

	public record Loan(@Min(1) int periodDays, @Min(1) int maxActive) {
	}
}
```

Add to `LibraryApplication`:

```java
@SpringBootApplication
@ConfigurationPropertiesScan
public class LibraryApplication {
```

(import `org.springframework.boot.context.properties.ConfigurationPropertiesScan`)

Append to `application.yml`:

```yaml
library:
  loan:
    period-days: 14
    max-active: 5
```

**Step 4: Run it to verify it passes**

Run: `cd backend && ./mvnw -q test -Dtest=LibraryPropertiesTest`
Expected: PASS

**Step 5: Commit**

```bash
git add backend
git commit -m "feat(backend): add library loan config properties"
```

---

### Task 4: Health check

**Files:**
- Test: `backend/src/test/java/com/library/HealthCheckTest.java`

**Step 1: Write the test**

```java
package com.library;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class HealthCheckTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void healthEndpointReportsUp() throws Exception {
		mockMvc.perform(get("/actuator/health"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("UP"));
	}
}
```

If `AutoConfigureMockMvc` doesn't resolve at that package, find it with
`unzip -l ~/.m2/repository/org/springframework/boot/spring-boot-webmvc-test/4.1.1/*.jar | grep AutoConfigureMockMvc`.

**Step 2: Run it**

Run: `cd backend && ./mvnw -q test -Dtest=HealthCheckTest`
Expected: PASS (actuator is already on the classpath and exposed in Task 2). If it fails, fix config before moving on.

**Step 3: Commit**

```bash
git add backend/src/test
git commit -m "test(backend): verify health endpoint reports UP"
```

---

### Task 5: Error response and exceptions

**Files:**
- Create: `backend/src/main/java/com/library/common/ErrorResponse.java`, `NotFoundException.java`, `BusinessRuleException.java`, `GlobalExceptionHandler.java` (all in `com/library/common/`)
- Test: `backend/src/test/java/com/library/common/GlobalExceptionHandlerTest.java`

**Step 1: Write the failing test**

A test-only controller throws each exception so the handler can be exercised before real controllers exist.

```java
package com.library.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.ThrowingController.class)
@Import({ GlobalExceptionHandlerTest.ThrowingController.class, GlobalExceptionHandler.class })
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
			.andExpect(jsonPath("$.timestamp").exists());
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

		@GetMapping("/test/boom")
		void boom() {
			throw new IllegalStateException("secret internals");
		}
	}
}
```

**Step 2: Run it to verify it fails**

Run: `cd backend && ./mvnw -q test -Dtest=GlobalExceptionHandlerTest`
Expected: compilation FAIL — `NotFoundException`, `BusinessRuleException`, `GlobalExceptionHandler` missing.

**Step 3: Implement**

`ErrorResponse.java`:

```java
package com.library.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(int status, String error, String message, Map<String, String> fieldErrors,
		Instant timestamp) {

	public static ErrorResponse of(int status, String error, String message) {
		return new ErrorResponse(status, error, message, null, Instant.now());
	}
}
```

(Boot 4 uses Jackson 3, but annotations stay in `com.fasterxml.jackson.annotation`.)

`NotFoundException.java`:

```java
package com.library.common;

public class NotFoundException extends RuntimeException {

	public NotFoundException(String message) {
		super(message);
	}
}
```

`BusinessRuleException.java`:

```java
package com.library.common;

public class BusinessRuleException extends RuntimeException {

	private final String code;

	public BusinessRuleException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
```

`GlobalExceptionHandler.java`:

```java
package com.library.common;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(NotFoundException.class)
	ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
		return respond(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
	}

	@ExceptionHandler(BusinessRuleException.class)
	ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex) {
		return respond(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage());
	}

	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
		return respond(HttpStatus.CONFLICT, "CONCURRENT_UPDATE",
				"The record was changed by someone else. Please retry.");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		ex.getBindingResult()
			.getFieldErrors()
			.forEach(e -> fieldErrors.putIfAbsent(e.getField(), e.getDefaultMessage()));
		ErrorResponse body = new ErrorResponse(400, "VALIDATION_FAILED", "Request validation failed", fieldErrors,
				Instant.now());
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
		return respond(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request body is missing or malformed");
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
		log.error("Unhandled exception", ex);
		return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error");
	}

	private ResponseEntity<ErrorResponse> respond(HttpStatus status, String error, String message) {
		return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), error, message));
	}
}
```

**Step 4: Run it to verify it passes**

Run: `cd backend && ./mvnw -q test -Dtest=GlobalExceptionHandlerTest`
Expected: 6 tests PASS

**Step 5: Commit**

```bash
git add backend/src
git commit -m "feat(backend): add global exception handler and error response"
```

---

### Task 6: Paged response DTO

**Files:**
- Create: `backend/src/main/java/com/library/common/PageResponse.java`
- Test: `backend/src/test/java/com/library/common/PageResponseTest.java`

**Step 1: Write the failing test**

```java
package com.library.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResponseTest {

	@Test
	void copiesContentAndPagingMetadataFromPage() {
		var page = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 5);

		PageResponse<String> response = PageResponse.from(page);

		assertThat(response.content()).containsExactly("a", "b");
		assertThat(response.page()).isEqualTo(1);
		assertThat(response.size()).isEqualTo(2);
		assertThat(response.totalElements()).isEqualTo(5);
		assertThat(response.totalPages()).isEqualTo(3);
	}
}
```

**Step 2: Run it to verify it fails**

Run: `cd backend && ./mvnw -q test -Dtest=PageResponseTest`
Expected: compilation FAIL — `PageResponse` missing.

**Step 3: Implement**

```java
package com.library.common;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
				page.getTotalPages());
	}
}
```

**Step 4: Run it to verify it passes**

Run: `cd backend && ./mvnw -q test -Dtest=PageResponseTest`
Expected: PASS

**Step 5: Commit**

```bash
git add backend/src
git commit -m "feat(backend): add paged response DTO"
```

---

### Task 7: JaCoCo coverage

**Files:**
- Modify: `backend/pom.xml` (`<build><plugins>`)

**Step 1: Add the plugin** after `spring-boot-maven-plugin`:

```xml
<plugin>
	<groupId>org.jacoco</groupId>
	<artifactId>jacoco-maven-plugin</artifactId>
	<version>0.8.13</version>
	<executions>
		<execution>
			<goals>
				<goal>prepare-agent</goal>
			</goals>
		</execution>
		<execution>
			<id>report</id>
			<phase>verify</phase>
			<goals>
				<goal>report</goal>
			</goals>
		</execution>
	</executions>
</plugin>
```

Check Maven Central for the latest JaCoCo release that supports Java 17 and use it if newer.

**Step 2: Run the full build**

Run: `cd backend && ./mvnw verify`
Expected: BUILD SUCCESS, all tests pass, `backend/target/site/jacoco/index.html` exists.

**Step 3: Commit**

```bash
git add backend/pom.xml
git commit -m "chore(backend): add JaCoCo coverage report"
```

---

### Task 8: Docs and finish

**Files:**
- Modify: `CLAUDE.md` (tech stack line → Spring Boot 4; note Windows `mvnw.cmd`), `README.md` (how to run backend)

**Step 1:** In `CLAUDE.md` change "Spring Boot 3" → "Spring Boot 4" everywhere; in the design doc's Decisions table update Runtime to Spring Boot 4.1.1.

**Step 2:** Replace `README.md` with a short project intro, prerequisites (Java 17, Node 22), and:

```bash
cd backend && ./mvnw spring-boot:run   # API on http://localhost:8080, H2 console at /h2-console
```

**Step 3: Commit**

```bash
git add CLAUDE.md README.md docs
git commit -m "docs: document backend setup and Spring Boot 4"
```

**Step 4: Ship** — run `/testing`, then `/pr-review`, fix Critical/Important findings, then `/raise-pr`.
