package com.coding.eventgateway.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public class AccountTransactionDto {

	private String eventId;
	private EventType type;
	private BigDecimal amount;
	private String currency;
	private Instant eventTimestamp;
	private Instant createdAt;
	private Map<String, Object> metadata;

	public String getEventId() {
		return eventId;
	}

	public EventType getType() {
		return type;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getCurrency() {
		return currency;
	}

	public Instant getEventTimestamp() {
		return eventTimestamp;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	public void setType(EventType type) {
		this.type = type;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public void setEventTimestamp(Instant eventTimestamp) {
		this.eventTimestamp = eventTimestamp;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}

}