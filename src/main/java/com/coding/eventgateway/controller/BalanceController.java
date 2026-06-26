package com.coding.eventgateway.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coding.eventgateway.dto.BalanceResponse;
import com.coding.eventgateway.service.EventGatewayService;

@RestController
@RequestMapping("/accounts")
public class BalanceController {

    @Autowired
    private EventGatewayService eventGatewayService;

    @GetMapping("/{accountId}/balance")
    public BalanceResponse balance(
            @PathVariable String accountId) {

        return eventGatewayService.getBalance(accountId);
    }

}