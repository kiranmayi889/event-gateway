package com.coding.eventgateway.exception;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

import com.coding.eventgateway.dto.ErrorResponse;
import com.coding.eventgateway.dto.EventType;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@Autowired
	private Tracer tracer;

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex) {

		ErrorResponse response = new ErrorResponse();

		response.setTimestamp(Instant.now());
		response.setStatus(HttpStatus.BAD_REQUEST.value());
		response.setError("Bad Request");
		response.setCode("VALIDATION_ERROR");
		response.setMessage(ex.getBindingResult().getFieldError().getDefaultMessage());

		response.setTraceId(getTraceId());

		return ResponseEntity.badRequest().body(response);
	}

	private String getTraceId() {
		Span span = tracer.currentSpan();
		return span != null ? span.context().traceId() : null;
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> duplicate(DataIntegrityViolationException ex) {

		ErrorResponse response = new ErrorResponse();

		response.setTimestamp(Instant.now());
		response.setStatus(HttpStatus.CONFLICT.value());
		response.setError("Conflict");
		response.setCode("DUPLICATE_EVENT");
		response.setMessage("Duplicate Event");

		response.setTraceId(getTraceId());

		return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
	}

	@ExceptionHandler(ResourceAccessException.class)
	public ResponseEntity<ErrorResponse> unavailable(ResourceAccessException ex) {

		ErrorResponse response = new ErrorResponse();

		response.setTimestamp(Instant.now());
		response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
		response.setError("Service Unavailable");
		response.setCode("ACCOUNT_SERVICE_DOWN");
		response.setMessage("Account Service is unavailable");

		response.setTraceId(getTraceId());

		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> generic(Exception ex) {

		ErrorResponse response = new ErrorResponse();

		response.setTimestamp(Instant.now());
		response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		response.setError("Internal Server Error");
		response.setCode("INTERNAL_ERROR");
		response.setMessage(ex.getMessage());

		response.setTraceId(getTraceId());

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	}

	@ExceptionHandler(AccountServiceUnavailableException.class)
	public ResponseEntity<ErrorResponse> handleAccountServiceUnavailable(AccountServiceUnavailableException ex) {

		ErrorResponse response = new ErrorResponse();

		response.setTimestamp(Instant.now());
		response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
		response.setError("Service Unavailable");
		response.setCode("ACCOUNT_SERVICE_UNAVAILABLE");
		response.setMessage(ex.getMessage());
		response.setTraceId(getTraceId());

		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleInvalidJson(HttpMessageNotReadableException ex) {

		ErrorResponse response = new ErrorResponse();

		response.setTimestamp(Instant.now());
		response.setStatus(HttpStatus.BAD_REQUEST.value());
		response.setError("Bad Request");
		response.setCode("INVALID_REQUEST");

		Throwable cause = ex.getMostSpecificCause();

		if (cause instanceof InvalidFormatException ife && ife.getTargetType() == EventType.class) {

			response.setMessage("Invalid event type. Allowed values are CREDIT and DEBIT.");

		} else {

			response.setMessage("Malformed request payload.");
		}

		response.setTraceId(getTraceId());

		return ResponseEntity.badRequest().body(response);
	}

}