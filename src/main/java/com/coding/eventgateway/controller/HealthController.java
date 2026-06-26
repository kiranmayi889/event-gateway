package com.coding.eventgateway.controller;


import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Event API")
public class HealthController {
	
	@Autowired
	private HealthEndpoint healthEndpoint;

    @GetMapping("/health")
    @Operation(summary = "Fetches Event Gateway Health")
    public ResponseEntity<Map<String, Object>> health() {

    	  HealthComponent health = healthEndpoint.health();

          Map<String, Object> response = new LinkedHashMap<>();
          response.put("status", health.getStatus().getCode());
          response.put("service", "event-gateway");
          response.put("details", health);

          return ResponseEntity.ok(response);

    }

}