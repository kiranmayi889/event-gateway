package com.coding.eventgateway.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Event Request")
public class EventRequest {

	@Schema(example = "evt-001")
	@NotBlank(message = "eventId is required")
	private String eventId;

	@Schema(example = "acct-123")
	@NotBlank(message = "accountId is required")
	private String accountId;

	@Schema(example = "CREDIT", allowableValues = { "CREDIT", "DEBIT" })
	@NotNull(message = "type is required")
	private EventType type;

	@Schema(example = "150.00")
	@NotNull(message = "amount is required")
	@DecimalMin(value = "0.0", inclusive = false, message = "amount must be greater than zero")
	private BigDecimal amount;

	@Schema(example = "USD")
	@NotBlank(message = "currency is required")
	private String currency;

	@Schema(example = "2026-05-15T14:02:11Z")
	@NotNull(message = "eventTimestamp is required")
	private Instant eventTimestamp;

	private Map<String, Object> metadata;

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public EventType getType() {
		return type;
	}

	public void setType(EventType type) {
		this.type = type;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public Instant getEventTimestamp() {
		return eventTimestamp;
	}

	public void setEventTimestamp(Instant eventTimestamp) {
		this.eventTimestamp = eventTimestamp;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}

}