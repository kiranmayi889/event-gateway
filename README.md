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
        Event Gateway Service
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

### Build Account Service

```bash
cd account-service
mvn clean install
```

### Build Event Gateway

```bash
cd ../event-gateway
mvn clean install
```

---

## Step 3 - Start Account Service

Open a terminal.

```bash
cd account-service
mvn spring-boot:run
```

Account Service starts on:

```text
http://localhost:8081
```

---

## Step 4 - Start Event Gateway

Open another terminal.

```bash
cd event-gateway
mvn spring-boot:run
```

Gateway starts on:

```text
http://localhost:8080
```

The Gateway communicates with the Account Service at:

```text
http://localhost:8081
```

---

## Verify Services

Gateway Health

```text
GET http://localhost:8080/actuator/health
```

Account Service Health

```text
GET http://localhost:8081/actuator/health
```

Expected response

```json
{
  "status": "UP"
}
```

---

# Running the Application using Spring Tool Suite (STS)

## Step 1 - Import Projects

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

## Step 2 - Run Account Service

1. Expand **account-service**.
2. Navigate to:

```text
src/main/java
```

3. Open:

```text
AccountServiceApplication.java
```

4. Right-click the file.
5. Select:

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

## Step 3 - Run Event Gateway

1. Expand **event-gateway**.
2. Navigate to:

```text
src/main/java
```

3. Open:

```text
EventGatewayApplication.java
```

4. Right-click the file.
5. Select:

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


# Running Tests

Run all tests

```bash
mvn test
```

Tests include:

* Unit Tests
* Controller Tests
* Integration Tests
* Trace Propagation Tests
* Resiliency Tests

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
