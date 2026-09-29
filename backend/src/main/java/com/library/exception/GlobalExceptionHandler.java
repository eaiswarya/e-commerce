package com.library.exception;

import com.library.dto.ErrorResponse;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

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

	/** A DB constraint caught what the service checks missed, e.g. two requests racing to create the same ISBN. */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
		log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
		return respond(HttpStatus.CONFLICT, "CONFLICT", "The request conflicts with existing data");
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

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		return respond(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Invalid value for parameter '" + ex.getName() + "'");
	}

	/** A {@code sort=} field that doesn't exist; the message omits the entity type name. */
	@ExceptionHandler(PropertyReferenceException.class)
	ResponseEntity<ErrorResponse> handleUnknownProperty(PropertyReferenceException ex) {
		return respond(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Unknown sort property '" + ex.getPropertyName() + "'");
	}

	@ExceptionHandler(AuthenticationException.class)
	ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
		String message = (ex instanceof BadCredentialsException) ? ex.getMessage() : "Authentication required";
		return respond(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
		return respond(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied");
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
		if (ex instanceof org.springframework.web.ErrorResponse frameworkError) {
			return handleFramework(frameworkError);
		}
		log.error("Unhandled exception", ex);
		return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error");
	}

	/**
	 * Spring MVC's own exceptions (unknown route, wrong method, missing parameter, ...)
	 * carry their proper status; keep it instead of turning them into 500s.
	 */
	private ResponseEntity<ErrorResponse> handleFramework(org.springframework.web.ErrorResponse ex) {
		HttpStatusCode code = ex.getStatusCode();
		HttpStatus status = HttpStatus.resolve(code.value());
		String error = (status != null) ? status.name() : "ERROR";
		String message = (status != null) ? status.getReasonPhrase() : "Request failed";
		return ResponseEntity.status(code)
			.headers(ex.getHeaders())
			.body(ErrorResponse.of(code.value(), error, message));
	}

	private ResponseEntity<ErrorResponse> respond(HttpStatus status, String error, String message) {
		return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), error, message));
	}
}
