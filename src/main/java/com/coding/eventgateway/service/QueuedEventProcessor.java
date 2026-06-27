package com.coding.eventgateway.service;

import java.time.Instant;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.coding.eventgateway.dto.Event;
import com.coding.eventgateway.dto.EventRequest;
import com.coding.eventgateway.dto.EventResponse;
import com.coding.eventgateway.dto.EventStatus;
import com.coding.eventgateway.repository.EventRepository;

@Component
public class QueuedEventProcessor {

	private static final Logger log = LoggerFactory.getLogger(QueuedEventProcessor.class);
    @Autowired
    private EventRepository repository;

    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${event.retry.max-attempts}")
    private int maxAttempts;

    @Value("${account.service.base-url}")
    private String accountServiceBaseUrl;
    
    private static final Random RANDOM = new Random();

    @Scheduled(fixedDelay = 30000) // every 30 seconds
    public void processQueuedEvents() {

        List<Event> queuedEvents =
                repository.findByStatus(EventStatus.QUEUED);
        
        Instant now = Instant.now();

        for (Event event : queuedEvents) {

        	 // Skip until the scheduled retry time
            if (event.getNextRetryTime() != null
                    && now.isBefore(event.getNextRetryTime())) {
                continue;
            }
            
            try {
            	log.info("Retrying queued event. EventId={}", event.getEventId());

                EventRequest request = convert(event);

                HttpHeaders headers = new HttpHeaders();
        		headers.setContentType(MediaType.APPLICATION_JSON);

        		HttpEntity<EventRequest> entity = new HttpEntity<>(request, headers);

                restTemplate.exchange(
                        accountServiceBaseUrl
                                + "/accounts/"
                                + event.getAccountId()
                                + "/transactions",
                        HttpMethod.POST,
                        entity,
                        EventResponse.class);

                event.setStatus(EventStatus.PROCESSED);
                repository.save(event);
                log.info("Queued event processed successfully. EventId={}", event.getEventId());

            } catch (Exception ex) {

            	int retry = event.getRetryCount() == null
                        ? 1
                        : event.getRetryCount() + 1;

                event.setRetryCount(retry);

                if (retry >= maxAttempts) {
                	log.error(
                            "Queued event failed after {} retry attempts. EventId={}",
                            retry,
                            event.getEventId());
                    event.setStatus(EventStatus.FAILED);

                } else {
                	long delay = calculateBackoff(retry);
                    event.setNextRetryTime(now.plusMillis(delay));
                	log.warn(
                            "Retry {} of {} failed for EventId={}. Will retry later.",
                            retry,
                            maxAttempts,
                            event.getEventId());
                    event.setStatus(EventStatus.QUEUED);
                }

                repository.save(event);
            }
        }
    }

    private EventRequest convert(Event event) {

        EventRequest request = new EventRequest();

        request.setEventId(event.getEventId());
        request.setAccountId(event.getAccountId());
        request.setAmount(event.getAmount());
        request.setCurrency(event.getCurrency());
        request.setType(event.getType());
        request.setEventTimestamp(event.getEventTimestamp());

        return request;
    }
    
    private long calculateBackoff(int retryCount) {

        long baseDelay = 1000L;       // 1 second
        long maxDelay = 30000L;       // 30 seconds

        long exponentialDelay =
                Math.min(baseDelay * (1L << (retryCount - 1)), maxDelay);

        long jitter = RANDOM.nextInt(1000);   // 0-999 ms

        return exponentialDelay + jitter;
    }
}