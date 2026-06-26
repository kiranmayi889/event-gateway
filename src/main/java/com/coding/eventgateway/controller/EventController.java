package com.coding.eventgateway.controller;


import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coding.eventgateway.dto.EventDto;
import com.coding.eventgateway.dto.EventRequest;
import com.coding.eventgateway.dto.EventResponse;
import com.coding.eventgateway.service.EventGatewayService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/events")
public class EventController {
	
	private static final Logger log =
            LoggerFactory.getLogger(EventController.class);

    @Autowired
    private EventGatewayService eventGatewayService;

    @PostMapping
    public ResponseEntity<EventResponse> publish(
            @Valid @RequestBody EventRequest request,
            HttpServletRequest servletRequest) {

        //String traceId = TraceIdUtil.getTraceId(servletRequest);

        return ResponseEntity.ok(
        		eventGatewayService.process(request, ""));
    }
    @GetMapping("/{eventId}")
    public EventDto getEvent(
            @PathVariable String eventId) {

        return eventGatewayService.getEvent(eventId);
    }

    @GetMapping
    public List<EventDto> getEvents(
            @RequestParam String account) {

        return eventGatewayService.getEvents(account);
    }
}