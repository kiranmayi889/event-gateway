package com.coding.eventgateway.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class AccountDetailsResponse {

    private String accountId;
    private BigDecimal balance;
    private Instant updatedAt;
    private List<AccountTransactionDto> recentTransactions;

    public String getAccountId() {
        return accountId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<AccountTransactionDto> getRecentTransactions() {
        return recentTransactions;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setRecentTransactions(List<AccountTransactionDto> recentTransactions) {
        this.recentTransactions = recentTransactions;
    }
}