package com.coding.eventgateway.dto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "event")
public class Event {

	@Id
	@Column(nullable = false, updatable = false)
	private String eventId;

	@Column(nullable = false)
	private String accountId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EventType type;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false)
	private String currency;

	@Column(nullable = false)
	private Instant eventTimestamp;

	@Lob
	private String metadataJson;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EventStatus status;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(name = "retry_count")
	private Integer retryCount = 0;

	@Column(name = "next_retry_time")
	private Instant nextRetryTime;

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

	public String getMetadataJson() {
		return metadataJson;
	}

	public void setMetadataJson(String metadataJson) {
		this.metadataJson = metadataJson;
	}

	public EventStatus getStatus() {
		return status;
	}

	public void setStatus(EventStatus status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Integer getRetryCount() {
		return retryCount;
	}

	public void setRetryCount(Integer retryCount) {
		this.retryCount = retryCount;
	}

	public Instant getNextRetryTime() {
		return nextRetryTime;
	}

	public void setNextRetryTime(Instant nextRetryTime) {
		this.nextRetryTime = nextRetryTime;
	}

}