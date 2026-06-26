package com.coding.eventgateway.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.coding.eventgateway.dto.BalanceResponse;
import com.coding.eventgateway.dto.Event;
import com.coding.eventgateway.dto.EventDto;
import com.coding.eventgateway.dto.EventRequest;
import com.coding.eventgateway.dto.EventResponse;
import com.coding.eventgateway.dto.EventStatus;
import com.coding.eventgateway.exception.AccountServiceUnavailableException;
import com.coding.eventgateway.repository.EventRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.annotation.PostConstruct;

@Service
public class EventGatewayService {

	private static final Logger log = LoggerFactory.getLogger(EventGatewayService.class);

	@Autowired
	private EventRepository repository;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private MeterRegistry meterRegistry;

	@Autowired
	private Tracer tracer;

	@Value("${account.service.base-url}")
	private String accountServiceBaseUrl;

	private Counter requestCounter;
	private Counter successCounter;
	private Counter failureCounter;
	private Counter duplicateCounter;

	@PostConstruct
	public void init() {

		requestCounter = Counter.builder("gateway.events.requests").description("Total event requests")
				.register(meterRegistry);

		successCounter = Counter.builder("gateway.events.success").description("Successfully processed events")
				.register(meterRegistry);

		failureCounter = Counter.builder("gateway.events.failed").description("Failed event processing")
				.register(meterRegistry);
		duplicateCounter = Counter.builder("gateway.events.duplicate").description("Duplicate event processing")
				.register(meterRegistry);
	}

	@Retry(name = "accountService")
	@CircuitBreaker(name = "accountService")
	public EventResponse process(EventRequest request) {
		log.info("Processing event started eventid= {} ", request.getEventId());
		requestCounter.increment();

		Event existing = repository.findById(request.getEventId()).orElse(null);

		/*
		 * Event already processed. Don't call Account Service again.
		 */
		if (existing != null) {

			if (existing.getStatus() == EventStatus.PROCESSED) {
				duplicateCounter.increment();
				log.info("Duplicate event found eventid= {} ", request.getEventId());

				EventResponse response = new EventResponse();

				response.setEventId(existing.getEventId());
				response.setAccountId(existing.getAccountId());
				response.setDuplicate(true);
				response.setTransactionStatus("PROCESSED");

				return response;
			}
		}
		Event event = new Event();
		event.setEventId(request.getEventId());
		event.setAccountId(request.getAccountId());
		event.setAmount(request.getAmount());
		event.setCurrency(request.getCurrency());
		event.setType(request.getType());
		event.setEventTimestamp(request.getEventTimestamp());
		event.setCreatedAt(Instant.now());
		event.setStatus(EventStatus.RECEIVED);
		event.setMetadataJson(toJson(request.getMetadata()));

		repository.save(event);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("X-Trace-Id", tracer.currentSpan() != null ? tracer.currentSpan().context().traceId() : "");

		HttpEntity<EventRequest> entity = new HttpEntity<>(request, headers);
		ResponseEntity<EventResponse> response;

		try {
			log.info("Account service call started eventid= {} ", request.getEventId());
			response = restTemplate.exchange(
					accountServiceBaseUrl + "/accounts/" + request.getAccountId() + "/transactions", HttpMethod.POST,
					entity, EventResponse.class);
			log.info("Account service call ended eventid= {} ", request.getEventId());
			event.setStatus(EventStatus.PROCESSED);
			repository.save(event);
			successCounter.increment();

		} catch (Exception e) {
			failureCounter.increment();
			log.error("Account service is unavailable eventid= {} ", request.getEventId());
			event.setStatus(EventStatus.FAILED);
			repository.save(event);
			throw new AccountServiceUnavailableException("Account Service is unavailable");
		}

		return response.getBody();
	}

	public EventDto getEvent(String eventId) {

		log.info("fetching requested event details from gateway eventid= {} ", eventId);
		Event event = repository.findById(eventId).orElseGet(() -> new Event());
		return convert(event);

	}

	public List<EventDto> getEvents(String accountId) {
		log.info("fetching event details from gateway for the given account accountid= {} ", accountId);

		return repository.findByAccountIdOrderByEventTimestampDesc(accountId).stream().map(this::convert)
				.collect(Collectors.toList());
	}

	@Retry(name = "accountService")
	@CircuitBreaker(name = "accountService")
	public BalanceResponse getBalance(String accountId) {
		try {

			log.info("fetching balance from account service for account accountid= {} ", accountId);
			return restTemplate.getForObject(accountServiceBaseUrl + "/accounts/" + accountId + "/balance",
					BalanceResponse.class);

		} catch (Exception ex) {
			log.error("error retrieved while fetching balance from account service for account accountid= {} ",
					accountId);
			throw new AccountServiceUnavailableException("Account Service is unavailable");
		}
	}

	private EventDto convert(Event event) {

		EventDto dto = new EventDto();

		dto.setEventId(event.getEventId());
		dto.setAccountId(event.getAccountId());
		dto.setAmount(event.getAmount());
		dto.setCurrency(event.getCurrency());
		dto.setType(event.getType());
		dto.setEventTimestamp(event.getEventTimestamp());
		dto.setStatus(event.getStatus());

		try {
			dto.setMetadata(objectMapper.readValue(event.getMetadataJson(), new TypeReference<Map<String, Object>>() {
			}));
		} catch (Exception ex) {
			dto.setMetadata(null);
		}

		return dto;
	}

	private String toJson(Map<String, Object> metadata) {

		try {
			return metadata == null ? null : objectMapper.writeValueAsString(metadata);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

}