package com.coding.eventgateway.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coding.eventgateway.dto.BalanceResponse;
import com.coding.eventgateway.service.EventGatewayService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/accounts")

@Tag(name = "Event API", description = "Operation related to Account Balance")
public class BalanceController {

    @Autowired
    private EventGatewayService eventGatewayService;

    @GetMapping("/{accountId}/balance")
    @Operation(summary = "Fetches account balance")
    public BalanceResponse balance(
            @PathVariable String accountId) {

        return eventGatewayService.getBalance(accountId);
    }

}