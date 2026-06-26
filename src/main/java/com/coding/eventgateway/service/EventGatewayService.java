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
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
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

@Service
public class EventGatewayService {

	private static final Logger log = LoggerFactory.getLogger(EventGatewayService.class);

	@Autowired
	private EventRepository repository;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	@Value("${account.service.base-url}")
	private String accountServiceBaseUrl;

	@Retryable(retryFor = {
			AccountServiceUnavailableException.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
	public EventResponse process(EventRequest request, String traceId) {

		Event existing = repository.findById(request.getEventId()).orElse(null);

		/*
		 * Event already processed. Don't call Account Service again.
		 */
		if (existing != null) {

			if (existing.getStatus() == EventStatus.PROCESSED) {

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
		headers.add("X-Trace-Id", traceId);
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<EventRequest> entity = new HttpEntity<>(request, headers);
		ResponseEntity<EventResponse> response;

		try {
			response = restTemplate.exchange(
					accountServiceBaseUrl + "/accounts/" + request.getAccountId() + "/transactions", HttpMethod.POST,
					entity, EventResponse.class);
			repository.save(event);
			event.setStatus(EventStatus.PROCESSED);
		} catch (Exception e) {
			event.setStatus(EventStatus.FAILED);
			repository.save(event);
			throw new AccountServiceUnavailableException("Account Service is unavailable");
		}

		return response.getBody();
	}

	@Recover
	public EventResponse recover(AccountServiceUnavailableException ex, EventRequest request, String traceId) {

		Event event = repository.findById(request.getEventId())
				.orElseThrow(() -> new AccountServiceUnavailableException(
						"Account Service is unavailable. Please try again later."));

		event.setStatus(EventStatus.FAILED);

		repository.save(event);

		throw new AccountServiceUnavailableException("Account Service is unavailable. Please try again later.");
	}

	public EventDto getEvent(String eventId) {

		Event event = repository.findById(eventId).orElseGet(() -> new Event());
		return convert(event);

	}

	public List<EventDto> getEvents(String accountId) {

		return repository.findByAccountIdOrderByEventTimestampDesc(accountId).stream().map(this::convert)
				.collect(Collectors.toList());
	}

	public BalanceResponse getBalance(String accountId) {
		try {
			return restTemplate.getForObject(accountServiceBaseUrl + "/accounts/" + accountId + "/balance",
					BalanceResponse.class);
		} catch (Exception ex) {
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