# Event Ledger Microservices

## Overview

This project implements a simple event ledger using two independent Spring Boot microservices:

1. **Event Gateway Service**
2. **Account Service**

Both services run independently with their own in-memory H2 database and communicate through REST APIs.

---

# Architecture Overview

## Event Gateway Service

Responsibilities:

* Accepts client event requests.
* Performs request validation.
* Stores received events in its local H2 database.
* Implements idempotency by checking duplicate event IDs.
* Invokes the Account Service to apply transactions.
* Retrieves account balances from the Account Service.
* Propagates trace IDs across service calls.
* Implements resiliency using Retry and Circuit Breaker.

Main APIs

* POST `/events`
* GET `/events/{eventId}`
* GET `/events?account={accountId}`
* GET `/balance/{accountId}`
* GET `/health`

---

## Account Service

Responsibilities:

* Maintains account balances.
* Stores transaction history.
* Creates accounts automatically when they do not exist.
* Handles duplicate transaction requests.
* Computes account balances.

Main APIs

* POST `/accounts/{accountId}/transactions`
* GET `/accounts/{accountId}/balance`
* GET `/accounts/{accountId}`
* GET `/health`

---

# Service Interaction

```text
                Client
                  │
                  ▼
        Event Gateway Service (H2 Database)
                  │
        REST API (HTTP)
                  │
                  ▼
          Account Service
                  │
            H2 Database
```

The Event Gateway stores event information locally and delegates balance computation to the Account Service.

Both services maintain independent databases and communicate only through REST APIs.

---

# Technologies

* Java 17
* Spring Boot
* Spring Web
* Spring Data JPA
* H2 Database
* Hibernate
* Bean Validation
* Micrometer Tracing / OpenTelemetry
* Resilience4j
* Spring Retry
* Spring Boot Actuator
* Swagger / OpenAPI
* JUnit 5
* Mockito
* MockMvc

---

# Project Structure

```
event-gateway/
account-service/
```

Each service is independently buildable and deployable.

---
# Setup Instructions

## Prerequisites

Ensure the following software is installed:

* Java 21
* Maven 3.9+
* Git
* Spring Tool Suite (STS) 4 or Eclipse with Spring Tools (optional)

---

# Running the Application using Command Line (Linux/macOS/Windows)

## Step 1 - Clone the Repository

```bash
git clone git@github.com:kiranmayi889/account-service.git
git clone git@github.com:kiranmayi889/event-gateway.git

git hub link - https://github.com/kiranmayi889
```

---

## Step 2 - Build the Projects

# Running the Application using Spring Tool Suite (STS)

## Step  - Import Projects

1. Open Spring Tool Suite (STS).
2. Select **File → Import**.
3. Choose **Maven → Existing Maven Projects**.
4. Browse to the project folder.
5. Select both projects:

   * account-service
   * event-gateway
6. Click **Finish**.
7. Wait for Maven dependencies to download.

---

## Step - Run Account Service

1. Right-click **accountservice** on the folder.
2. Select:

```text
Run As → Spring Boot App
```

Wait until the console displays:

```text
Started AccountServiceApplication
```

The service will be available at:

```text
http://localhost:8081
```

---

## Step  - Run Event Gateway

1. Right-click on **event-gateway** on the folder.
2. Select:

```text
Run As → Spring Boot App
```

Wait until the console displays:

```text
Started EventGatewayApplication
```

The service will be available at:

```text
http://localhost:8080
```

---

# Swagger UI

### Event Gateway

```text
http://localhost:8080/swagger-ui/index.html
```

### Account Service

```text
http://localhost:8081/swagger-ui/index.html
```

---

# H2 Console

### Event Gateway

```text
http://localhost:8080/h2-console
```

### Account Service

```text
http://localhost:8081/h2-console
```

---

# Running Tests

## Using Maven

### Event Gateway

```bash
cd event-gateway
mvn test
```

### Account Service

```bash
cd account-service
mvn test
```

---

## Using Spring Tool Suite (STS)

### Run All Tests

Right-click the project.

Select:

```text
Run As → Maven test
```

or

```text
Run As → JUnit Test
```

You can also run an individual test class by right-clicking the test class and selecting:

```text
Run As → JUnit Test
```

Tests include:

* Unit Tests
* Controller Tests
* Integration Tests
* Trace Propagation Tests
* Resiliency Tests

---

# Custom Metrics

The application exposes custom application metrics using **Micrometer**.

These metrics can be viewed through the Spring Boot Actuator metrics endpoint.

## Event Gateway Metrics

| Metric                     | Description                                            |
| -------------------------- | ------------------------------------------------------ |
| `gateway.events.requests`  | Total number of event requests received by the Gateway |
| `gateway.events.success`   | Number of successfully processed events                |
| `gateway.events.failed`    | Number of failed event processing attempts             |
| `gateway.events.duplicate` | Number of duplicate (idempotent) event requests        |

## Account Service Metrics

| Metric                           | Description                                       |
| -------------------------------- | ------------------------------------------------- |
| `account.transactions.requests`  | Total number of transaction requests received     |
| `account.transactions.success`   | Number of successfully processed transactions     |
| `account.transactions.failed`    | Number of failed transaction processing attempts  |
| `account.transactions.duplicate` | Number of duplicate transaction requests detected |

## Viewing Metrics

Metrics are available through the Spring Boot Actuator endpoint.

### Event Gateway

```text
GET http://localhost:8080/actuator/metrics
```

Example:

```text
GET http://localhost:8080/actuator/metrics/gateway.events.requests
```

### Account Service

```text
GET http://localhost:8081/actuator/metrics
```

Example:

```text
GET http://localhost:8081/actuator/metrics/account.transactions.requests
```

These metrics help monitor request volume, successful processing, failures, and duplicate requests, providing operational insight into both services.


---

# Trace Propagation

The Gateway generates or retrieves the current trace using Micrometer Tracing/OpenTelemetry.

The trace ID is propagated to the Account Service using the HTTP header:

```
X-Trace-Id
```

Both services include the trace ID in their structured logs.

---

# Observability

The application exposes metrics using Micrometer.

Custom metrics include:

* gateway.events.requests

* gateway.events.success

* gateway.events.failed

* gateway.events.duplicate

* account.transactions.requests

* account.transactions.success

* account.transactions.failed

* account.transactions.duplicate

Health endpoints are exposed through Spring Boot Actuator.
example: http://localhost:8080/actuator/metrics/gateway.events.requests - GET
---

# Resiliency Pattern

The Gateway uses **Retry** together with **Circuit Breaker**.

### Retry

Transient failures such as temporary network interruptions or short service outages are handled using Retry.

The Gateway retries failed Account Service requests before returning an error.

### Circuit Breaker

If repeated failures occur, the Circuit Breaker opens and temporarily stops sending requests to the Account Service.

Benefits:

* Prevents cascading failures.
* Reduces unnecessary network traffic.
* Allows the downstream service time to recover.
* Improves application responsiveness during outages.

Once the configured wait duration expires, the Circuit Breaker transitions to HALF_OPEN to determine whether the Account Service has recovered.

---

# Idempotency

Both services support idempotent processing.

Duplicate requests with the same Event ID are detected and ignored, preventing duplicate balance updates.

---

# Event Ordering

Events may arrive out of chronological order.

Transactions are stored independently of arrival order and are returned ordered by Event Timestamp.

Balances are computed correctly regardless of event arrival order.

---

# Logging

Structured JSON logging is implemented.

Each log entry includes:

* Timestamp
* Log Level
* Service Name
* Trace ID
* Message

This enables end-to-end request tracing across both services.

---

# Assumptions

* Event IDs are globally unique.
* Amounts are positive values.
* Supported transaction types are CREDIT and DEBIT.
* Each service owns its own database.
* Services communicate only through REST APIs.

-------------------------------------------------------------------------------------------

# Additional Features Implemented

Beyond the mandatory assignment requirements, the following production-grade enhancements have been implemented to improve resilience, observability, and reliability.

## 1. OpenTelemetry + OpenTelemetry Collector + Jaeger

Implemented end-to-end distributed tracing using OpenTelemetry.

### Features

* Automatic trace generation for incoming HTTP requests.
* Trace context propagation from **Event Gateway** to **Account Service**.
* OpenTelemetry Collector receives traces from both services.
* Jaeger is used for distributed trace visualization.
* Every request can be traced across both microservices using a single Trace ID.

### Access

Jaeger UI:

```text
http://localhost:16686
```

---

## 2. Prometheus Metrics Endpoint

Both services expose Prometheus-compatible metrics using Spring Boot Actuator and Micrometer.

### Gateway Metrics

* gateway.events.requests
* gateway.events.success
* gateway.events.failed
* gateway.events.duplicate

### Account Service Metrics

* account.transactions.requests
* account.transactions.success
* account.transactions.failed
* account.transactions.duplicate

### Endpoints

Gateway

```text
http://localhost:8080/actuator/prometheus
```

Account Service

```text
http://localhost:8081/actuator/prometheus
```

These endpoints can be scraped by any Prometheus-compatible monitoring system.

---

## 3. Retry with Exponential Backoff and Jitter

Resilience4j Retry has been configured to automatically retry transient failures when the Account Service is unavailable.

### Features

* Configurable retry attempts.
* Exponential backoff between retries.
* Randomized jitter to prevent synchronized retry storms.
* Automatic retry before reporting failure to the client.

This improves resilience against temporary network failures and service outages.

---

## 4. Gateway Rate Limiting

The Event Gateway implements request rate limiting to protect downstream services from excessive traffic.

### Features

* Limits the number of requests accepted within a configured time window.
* Returns **HTTP 429 (Too Many Requests)** when the limit is exceeded.
* Prevents accidental overload of the Gateway and Account Service.

---

## 5. Asynchronous Fallback for Account Service Failures

When the Account Service is temporarily unavailable, the Gateway does not immediately discard the request.

### Workflow

1. The incoming event is persisted locally.
2. The event status is updated to **QUEUED**.
3. A scheduler periodically retries queued events.
4. When the Account Service becomes available, queued events are automatically processed.
5. Successfully processed events are marked as **PROCESSED**.
6. Events that exceed the configured retry threshold are marked as **FAILED**.

### Retry Policy

* Configurable maximum retry attempts.
* Exponential retry scheduling.
* Retry count maintained for each queued event.
* Failed events are retained for operational visibility and troubleshooting.

This approach provides eventual consistency while ensuring events are not lost during temporary downstream outages.

---

## 6. Observability

The solution provides comprehensive observability through:

* Distributed tracing with OpenTelemetry and Jaeger.
* Prometheus-compatible metrics.
* Custom Micrometer counters.
* Health endpoints using Spring Boot Actuator.
* Structured application logging with Trace IDs for end-to-end request correlation.

These capabilities make the system easier to monitor, troubleshoot, and operate in production environments.
