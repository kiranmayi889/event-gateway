package com.coding.eventgateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.coding.eventgateway.dto.BalanceResponse;
import com.coding.eventgateway.dto.Event;
import com.coding.eventgateway.dto.EventDto;
import com.coding.eventgateway.dto.EventRequest;
import com.coding.eventgateway.dto.EventResponse;
import com.coding.eventgateway.dto.EventStatus;
import com.coding.eventgateway.dto.EventType;
import com.coding.eventgateway.exception.AccountServiceUnavailableException;
import com.coding.eventgateway.repository.EventRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

@ExtendWith(MockitoExtension.class)
class EventGatewayServiceTest {

	@Mock
	private EventRepository repository;

	@Mock
	private RestTemplate restTemplate;

	@Mock
	private Tracer tracer;

	@Mock
	private ObjectMapper objectMapper;

	@InjectMocks
	private EventGatewayService service;

	@BeforeEach
	void setup() {

		ReflectionTestUtils.setField(service, "meterRegistry", new SimpleMeterRegistry());

		ReflectionTestUtils.setField(service, "accountServiceBaseUrl", "http://localhost:8081");

		service.init();
	}

	private EventRequest request() {

		EventRequest request = new EventRequest();

		request.setEventId("EVT-100");
		request.setAccountId("ACC-1");
		request.setAmount(BigDecimal.valueOf(100));
		request.setCurrency("USD");
		request.setType(EventType.CREDIT);
		request.setEventTimestamp(Instant.now());
		request.setMetadata(Map.of("source", "ATM"));

		return request;
	}

	@Test
	void shouldProcessNewEventSuccessfully() throws Exception {

		EventRequest request = request();

		when(repository.findById("EVT-100")).thenReturn(Optional.empty());

		when(objectMapper.writeValueAsString(any())).thenReturn("{}");

		EventResponse accountResponse = new EventResponse();
		accountResponse.setAccountId("ACC-1");
		accountResponse.setEventId("EVT-100");
		accountResponse.setTransactionStatus("PROCESSED");

		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(EventResponse.class)))
				.thenReturn(ResponseEntity.ok(accountResponse));

		EventResponse response = service.process(request);

		assertNotNull(response);
		assertEquals("EVT-100", response.getEventId());
		assertEquals("ACC-1", response.getAccountId());

		verify(repository, times(2)).save(any(Event.class));

		verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
				eq(EventResponse.class));
	}

	@Test
	void shouldReturnDuplicateWhenAlreadyProcessed() {

		Event existing = new Event();

		existing.setEventId("EVT-100");
		existing.setAccountId("ACC-1");
		existing.setStatus(EventStatus.PROCESSED);

		when(repository.findById("EVT-100")).thenReturn(Optional.of(existing));

		EventResponse response = service.process(request());

		assertTrue(response.isDuplicate());

		assertEquals("PROCESSED", response.getTransactionStatus());

		verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(EventResponse.class));
	}

	@Test
	void shouldReturnEventById() throws Exception {

		Event event = new Event();

		event.setEventId("EVT-100");
		event.setAccountId("ACC-1");
		event.setAmount(BigDecimal.valueOf(100));
		event.setCurrency("USD");
		event.setMetadataJson("{\"source\":\"ATM\"}");

		when(repository.findById("EVT-100")).thenReturn(Optional.of(event));

		when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(Map.of("source", "ATM"));

		EventDto dto = service.getEvent("EVT-100");

		assertEquals("EVT-100", dto.getEventId());

		assertEquals("ACC-1", dto.getAccountId());

		assertEquals("ATM", dto.getMetadata().get("source"));
	}

	@Test
	void shouldReturnEmptyEventWhenNotFound() {

		when(repository.findById(anyString())).thenReturn(Optional.empty());

		EventDto dto = service.getEvent("EVT-100");

		assertNull(dto.getEventId());
	}

	@Test
	void shouldReturnEventsForAccount() throws Exception {

		Event e1 = new Event();
		e1.setEventId("E1");
		e1.setMetadataJson("{}");

		Event e2 = new Event();
		e2.setEventId("E2");
		e2.setMetadataJson("{}");

		when(repository.findByAccountIdOrderByEventTimestampDesc("ACC-1")).thenReturn(List.of(e1, e2));

		when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(Collections.emptyMap());

		List<EventDto> events = service.getEvents("ACC-1");

		assertEquals(2, events.size());

		assertEquals("E1", events.get(0).getEventId());

		assertEquals("E2", events.get(1).getEventId());
	}

	@Test
	void shouldReturnBalanceSuccessfully() {

		BalanceResponse response = new BalanceResponse();
		response.setAccountId("ACC-1");
		response.setBalance(BigDecimal.valueOf(500));

		when(restTemplate.getForObject(anyString(), eq(BalanceResponse.class))).thenReturn(response);

		BalanceResponse result = service.getBalance("ACC-1");

		assertNotNull(result);
		assertEquals("ACC-1", result.getAccountId());
		assertEquals(BigDecimal.valueOf(500), result.getBalance());

		verify(restTemplate, times(1)).getForObject(anyString(), eq(BalanceResponse.class));
	}

	@Test
	void shouldThrowExceptionWhenBalanceServiceUnavailable() {

		when(restTemplate.getForObject(anyString(), eq(BalanceResponse.class)))
				.thenThrow(new ResourceAccessException("Connection refused"));

		assertThrows(AccountServiceUnavailableException.class, () -> service.getBalance("ACC-1"));

		verify(restTemplate, times(1)).getForObject(anyString(), eq(BalanceResponse.class));
	}

	@Test
	void shouldReturnNullMetadataWhenJsonParsingFails() throws Exception {

		Event event = new Event();
		event.setEventId("EVT-100");
		event.setMetadataJson("INVALID");

		when(repository.findById("EVT-100")).thenReturn(Optional.of(event));

		when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenThrow(new RuntimeException());

		EventDto dto = service.getEvent("EVT-100");

		assertNotNull(dto);
		assertNull(dto.getMetadata());
	}

	@Test
	void shouldThrowExceptionWhenAccountServiceUnavailableDuringProcess() throws Exception {

		EventRequest request = request();

		when(repository.findById(anyString())).thenReturn(Optional.empty());

		when(objectMapper.writeValueAsString(any())).thenReturn("{}");

		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(EventResponse.class)))
				.thenThrow(new ResourceAccessException("Account Service Down"));

		assertThrows(AccountServiceUnavailableException.class, () -> service.process(request));

		verify(repository, atLeast(2)).save(any(Event.class));
	}

	@Test
	void shouldSaveFailedStatusWhenAccountServiceFails() throws Exception {

		EventRequest request = request();

		when(repository.findById(anyString())).thenReturn(Optional.empty());

		when(objectMapper.writeValueAsString(any())).thenReturn("{}");

		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(EventResponse.class)))
				.thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

		assertThrows(AccountServiceUnavailableException.class, () -> service.process(request));

		ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);

		verify(repository, times(2)).save(captor.capture());

		List<Event> savedEvents = captor.getAllValues();

		assertEquals(EventStatus.FAILED, savedEvents.get(0).getStatus());
	}

	@Test
	void shouldIncrementSuccessCounter() throws Exception {

		EventRequest request = request();

		when(repository.findById(anyString())).thenReturn(Optional.empty());

		when(objectMapper.writeValueAsString(any())).thenReturn("{}");

		EventResponse accountResponse = new EventResponse();
		accountResponse.setEventId("EVT-100");

		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(EventResponse.class)))
				.thenReturn(ResponseEntity.ok(accountResponse));

		service.process(request);

		SimpleMeterRegistry registry = (SimpleMeterRegistry) ReflectionTestUtils.getField(service, "meterRegistry");

		assertEquals(1.0, registry.counter("gateway.events.success").count());
	}

	@Test
	void shouldIncrementFailureCounter() throws Exception {

		EventRequest request = request();

		when(repository.findById(anyString())).thenReturn(Optional.empty());

		when(objectMapper.writeValueAsString(any())).thenReturn("{}");

		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(EventResponse.class)))
				.thenThrow(new RuntimeException());

		try {
			service.process(request);
		} catch (Exception ignored) {
		}

		SimpleMeterRegistry registry = (SimpleMeterRegistry) ReflectionTestUtils.getField(service, "meterRegistry");

		assertEquals(0.0, registry.counter("gateway.events.failed").count());
	}

	@Test
	void shouldPropagateTraceIdToAccountService() throws Exception {

		EventRequest request = request();

		when(repository.findById(anyString())).thenReturn(Optional.empty());

		when(objectMapper.writeValueAsString(any())).thenReturn("{}");

		EventResponse response = new EventResponse();
		response.setEventId("EVT-100");

		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(EventResponse.class)))
				.thenReturn(ResponseEntity.ok(response));

		// Micrometer tracing mocks
		Span span = mock(Span.class);
		io.micrometer.tracing.TraceContext context = mock(io.micrometer.tracing.TraceContext.class);

		when(tracer.currentSpan()).thenReturn(span);
		when(span.context()).thenReturn(context);
		when(context.traceId()).thenReturn("trace-12345");

		ArgumentCaptor<HttpEntity<EventRequest>> captor = ArgumentCaptor.forClass(HttpEntity.class);

		service.process(request);

		verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), captor.capture(), eq(EventResponse.class));

		HttpHeaders headers = captor.getValue().getHeaders();

		assertEquals("trace-12345", headers.getFirst("X-Trace-Id"));
	}

	@Test
	void shouldSendEmptyTraceIdWhenCurrentSpanIsNull() throws Exception {

		EventRequest request = request();

		when(repository.findById(anyString())).thenReturn(Optional.empty());

		when(objectMapper.writeValueAsString(any())).thenReturn("{}");

		when(tracer.currentSpan()).thenReturn(null);

		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(EventResponse.class)))
				.thenReturn(ResponseEntity.ok(new EventResponse()));

		ArgumentCaptor<HttpEntity<EventRequest>> captor = ArgumentCaptor.forClass(HttpEntity.class);

		service.process(request);

		verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), captor.capture(), eq(EventResponse.class));

		assertEquals("", captor.getValue().getHeaders().getFirst("X-Trace-Id"));
	}
}