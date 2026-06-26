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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/events")
@Tag(name = "Event API", description = "Operations related to Event Gateway")
public class EventController {
	
	private static final Logger log =
            LoggerFactory.getLogger(EventController.class);

    @Autowired
    private EventGatewayService eventGatewayService;

    @PostMapping
    @Operation(summary = "Submit an Event")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event Created"),
            @ApiResponse(responseCode = "400", description = "Invalid Request"),
            @ApiResponse(responseCode = "503", description = "Account Service Unavailable")
    })
    public ResponseEntity<EventResponse> publish(
            @Valid @RequestBody EventRequest request,
            HttpServletRequest servletRequest) {

        return ResponseEntity.ok(
        		eventGatewayService.process(request));
    }
    @GetMapping("/{eventId}")
    @Operation(summary = "Get Event by ID")
    public EventDto getEvent(
            @PathVariable String eventId) {

        return eventGatewayService.getEvent(eventId);
    }

    @GetMapping
    @Operation(summary = "List Events by Account")
    public List<EventDto> getEvents(
            @RequestParam String account) {

        return eventGatewayService.getEvents(account);
    }
}