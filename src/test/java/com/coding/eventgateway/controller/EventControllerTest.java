package com.coding.eventgateway.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.coding.eventgateway.dto.EventDto;
import com.coding.eventgateway.dto.EventResponse;
import com.coding.eventgateway.exception.AccountServiceUnavailableException;
import com.coding.eventgateway.service.EventGatewayService;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;

@WebMvcTest(EventController.class)
@AutoConfigureMockMvc(addFilters = false)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventGatewayService eventGatewayService;
    
    @MockBean
    private Tracer tracer;

    @BeforeEach
    void setup() {

        Span span = mock(Span.class);
        TraceContext context = mock(TraceContext.class);

        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(context);
        when(context.traceId()).thenReturn("trace-123");
    }
    @Test
    void shouldPublishEvent() throws Exception {

        EventResponse response = new EventResponse();
        response.setEventId("EVT-100");
        response.setAccountId("ACC-1");
        response.setDuplicate(false);
        response.setTransactionStatus("PROCESSED");

        when(eventGatewayService.process(any()))
                .thenReturn(response);

        String request = """
                {
                  "eventId":"EVT-100",
                  "accountId":"ACC-1",
                  "amount":100,
                  "currency":"USD",
                  "type":"CREDIT",
                  "eventTimestamp":"2026-06-27T10:00:00Z"
                }
                """;

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("EVT-100"))
                .andExpect(jsonPath("$.accountId").value("ACC-1"))
                .andExpect(jsonPath("$.transactionStatus").value("PROCESSED"))
                .andExpect(jsonPath("$.duplicate").value(false));

        verify(eventGatewayService).process(any());
    }

    @Test
    void shouldReturnDuplicateEvent() throws Exception {

        EventResponse response = new EventResponse();
        response.setEventId("EVT-100");
        response.setAccountId("ACC-1");
        response.setDuplicate(true);
        response.setTransactionStatus("PROCESSED");

        when(eventGatewayService.process(any()))
                .thenReturn(response);

        String request = """
                {
                  "eventId":"EVT-100",
                  "accountId":"ACC-1",
                  "amount":100,
                  "currency":"USD",
                  "type":"CREDIT",
                  "eventTimestamp":"2026-06-27T10:00:00Z"
                }
                """;

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicate").value(true));
    }

    @Test
    void shouldReturn503WhenAccountServiceUnavailable() throws Exception {

        when(eventGatewayService.process(any()))
                .thenThrow(new AccountServiceUnavailableException("Account Service is unavailable"));

        String request = """
                {
                  "eventId":"EVT-100",
                  "accountId":"ACC-1",
                  "amount":100,
                  "currency":"USD",
                  "type":"CREDIT",
                  "eventTimestamp":"2026-06-27T10:00:00Z"
                }
                """;

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void shouldGetEventById() throws Exception {

        EventDto dto = new EventDto();
        dto.setEventId("EVT-100");
        dto.setAccountId("ACC-1");
        dto.setAmount(BigDecimal.valueOf(100));

        when(eventGatewayService.getEvent("EVT-100"))
                .thenReturn(dto);

        mockMvc.perform(get("/events/EVT-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("EVT-100"))
                .andExpect(jsonPath("$.accountId").value("ACC-1"));
    }

    @Test
    void shouldGetEventsByAccount() throws Exception {

        EventDto dto = new EventDto();
        dto.setEventId("EVT-100");
        dto.setAccountId("ACC-1");
        dto.setAmount(BigDecimal.valueOf(100));
        dto.setEventTimestamp(Instant.now());

        when(eventGatewayService.getEvents("ACC-1"))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/events")
                .param("account", "ACC-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value("EVT-100"));
    }

    @Test
    void shouldReturnBadRequestWhenRequestIsInvalid() throws Exception {

        String request = """
                {
                  "accountId":"ACC-1",
                  "amount":-10,
                  "currency":"USD"
                }
                """;

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestForInvalidEnum() throws Exception {

        String request = """
                {
                  "eventId":"EVT-100",
                  "accountId":"ACC-1",
                  "amount":100,
                  "currency":"USD",
                  "type":"INVALID",
                  "eventTimestamp":"2026-06-27T10:00:00Z"
                }
                """;

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest());
    }
}