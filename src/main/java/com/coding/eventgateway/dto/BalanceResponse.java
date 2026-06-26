package com.coding.eventgateway.dto;

import java.math.BigDecimal;

public class BalanceResponse {

	private String accountId;

	private BigDecimal balance;

	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	// getters & setters
}