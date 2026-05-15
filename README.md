# customer-onboarding-platform

> A hands-on Spring Boot microservices reference built with service discovery, an API gateway, async messaging, and distributed tracing.

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-brightgreen)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2024.0.1-blue)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-ready-orange)
![Docker](https://img.shields.io/badge/Docker-ready-blue)
![License](https://img.shields.io/badge/license-MIT-green)

I built this to get my hands dirty with microservices — not just read about them. The system handles customer registration across five independent services that communicate through both REST and RabbitMQ. Each service has its own PostgreSQL database, registers itself with Eureka so others can find it by name, and all traffic comes in through a single API Gateway. I also wired up Zipkin so I can actually follow a request as it hops across services. Images are built and pushed to Docker Hub with Jib, and I'm currently working on migrating the whole thing to Kubernetes.

---

## 📑 Table of Contents

- [Architecture](#️-architecture)
- [Services](#-services)
- [Project Structure](#️-project-structure)
- [Getting Started](#-getting-started)
- [Running the Infrastructure](#-running-the-infrastructure)
- [API Reference](#-api-reference)
- [Roadmap](#️-roadmap)
- [Acknowledgements](#-acknowledgements)
- [License](#-license)

---

## 🏛️ Architecture

All external traffic enters through the **API Gateway** (port `9090`). The gateway resolves service locations via **Eureka** and load-balances requests across available instances. When a customer registers, the **Customer** service synchronously calls the **Fraud** service over the service mesh to check for fraud. If fraud is detected, a notification event is published to a RabbitMQ **topic exchange**; the **Notification** service consumes it asynchronously and persists the notification record. Distributed traces across all hops are sent to **Zipkin**.

```
Client
  │
  ▼
API Gateway (:9090)        ← Spring Cloud Gateway + Eureka discovery
  │
  ▼
Customer Service (:8080)   ← registers customer, orchestrates flow
  │        │
  │        └─── HTTP (load-balanced) ──► Fraud Service (:8081)
  │                                          └── persists FraudCheckHistory → PostgreSQL
  │
  └─── RabbitMQ publish ──► internal.exchange
                                   │
                              notification.queue
                                   │
                                   ▼
                          Notification Service (:8085)
                               └── persists Notification → PostgreSQL

Supporting infrastructure: Eureka Server (:8761) · Zipkin (:9411) · pgAdmin (:5050)
```

---

## 🧩 Services

### `eureka-server`
Netflix Eureka service registry. All services register on startup and resolve each other by logical name (e.g. `http://FRAUD`) instead of hardcoded URLs. Does not register with itself.

### `apigw`
Spring Cloud Gateway running on port `9090`. Registered with Eureka and routes inbound requests to downstream services using load-balanced URIs (`lb://CUSTOMER`). Currently routes:

| Predicate | Target |
|-----------|--------|
| `Path=/api/v1/customers/**` | `lb://CUSTOMER` |

### `customer`
The business entry point. Accepts a customer registration request, persists the customer to its own PostgreSQL database, and then:
1. Calls the **Fraud** service via a declarative `@HttpExchange` interface backed by a load-balanced `RestClient`.
2. If the customer is flagged as a fraudster, publishes a `NotificationRequest` to RabbitMQ (`internal.exchange` → `internal.notification.routing-key`) and deletes the customer record.
3. If the customer is clean, the record remains.

### `fraud`
Fraud detection service. Exposes `GET /api/v1/fraud-check/{id}`, saves a `FraudCheckHistory` row (customerId, isFraudster, createdAt) to its own PostgreSQL schema, and returns the result. Currently marks every customer as a fraudster — this is intentional as a stub for extending real detection logic.

### `notification`
Notification persistence service. Receives events from RabbitMQ via `@RabbitListener` on `notification.queue` and saves a `Notification` record (toCustomerId, toCustomerEmail, sender, message, sentAt) to its own PostgreSQL schema. Also exposes a direct `POST /api/v1/notification` HTTP endpoint.

### `amqp`
Shared library module (not a deployable service). Provides:
- `RabbitMQConfig` — configures the `AmqpTemplate` and `SimpleRabbitListenerContainerFactory` with a Jackson JSON message converter.
- `RabbitMQMessageProducer` — a reusable `publish(exchange, routingKey, payload)` helper used by any service that needs to send messages.

---

## 🗂️ Project Structure

```
spring-microservices/
├── amqp/                         # Shared RabbitMQ config and producer (library module)
│   └── src/main/java/com/amigoscode/amqp/
│       ├── RabbitMQConfig.java
│       └── RabbitMQMessageProducer.java
├── apigw/                        # Spring Cloud API Gateway
│   └── src/main/java/com/amigoscode/apigateway/
│       └── ApiGatewayApplication.java
├── customer/                     # Customer registration service
│   └── src/main/java/com/amigoscode/customer/
│       ├── config/               # RestClient beans for Fraud and Notification
│       ├── service/              # Declarative HTTP interfaces (FraudHttpService)
│       ├── Customer.java
│       ├── CustomerController.java
│       ├── CustomerService.java
│       └── CustomerRepository.java
├── fraud/                        # Fraud detection service
│   └── src/main/java/com/amigoscode/fraud/
│       ├── FraudCheckHistory.java
│       ├── FraudCheckService.java
│       └── FraudController.java
├── notification/                 # Notification persistence service
│   └── src/main/java/com/amigoscode/notification/
│       ├── rabbitmq/             # RabbitMQ consumer
│       ├── NotificationConfig.java
│       ├── NotificationService.java
│       └── NotificationController.java
├── eureka-server/                # Eureka service registry
├── docker-compose.yml            # PostgreSQL, pgAdmin, RabbitMQ, Zipkin, Eureka, Gateway
└── pom.xml                       # Parent POM with shared dependency management
```

---

## 🚀 Getting Started

### Prerequisites

| Tool | Version |
|------|---------|
| Java (JDK) | 21 |
| Maven | 3.9+ or the included `mvnw` wrapper |
| Docker & Docker Compose | Any recent version |

### Installation

1. Clone the repository.

```bash
git clone https://github.com/<your-username>/spring-microservices.git
cd spring-microservices
```

2. Build all modules.

```bash
./mvnw clean package -DskipTests
```

3. Start the infrastructure, then run each service. See the sections below for details.

---

## 🐳 Running the Infrastructure

Start all supporting infrastructure (PostgreSQL, pgAdmin, RabbitMQ, Zipkin, Eureka, and the API Gateway) with a single command:

```bash
docker-compose up -d
```

| Service | URL |
|---------|-----|
| Eureka Dashboard | http://localhost:8761 |
| API Gateway | http://localhost:9090 |
| RabbitMQ Management | http://localhost:15672 (guest / guest) |
| Zipkin | http://localhost:9411 |
| pgAdmin | http://localhost:5050 |

### Running services locally

Start each service after the infrastructure is up. The services must be started in order: Eureka → then any order.

```bash
# In separate terminals:
./mvnw spring-boot:run -pl eureka-server
./mvnw spring-boot:run -pl customer
./mvnw spring-boot:run -pl fraud
./mvnw spring-boot:run -pl notification
./mvnw spring-boot:run -pl apigw
```

### Building Docker images with Jib

Each service is configured with the Jib Maven plugin targeting `eclipse-temurin:21`. Run the following to build and push all images to Docker Hub (requires you to be logged in):

```bash
./mvnw compile jib:build
```

Or build to a local Docker daemon without pushing:

```bash
./mvnw compile jib:dockerBuild
```

---

## 📖 API Reference

All requests go through the API Gateway at `http://localhost:9090`.

### Customers

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/customers` | Register a new customer. Triggers fraud check and, if flagged, sends a notification via RabbitMQ. |

**POST `/api/v1/customers` request body**

| Field | Type | Description |
|-------|------|-------------|
| `firstName` | `string` | Customer first name. |
| `lastName` | `string` | Customer last name. |
| `email` | `string` | Customer email address. |

**Example**

```bash
curl -X POST http://localhost:9090/api/v1/customers \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Jane","lastName":"Doe","email":"jane@example.com"}'
```

### Fraud (internal)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/fraud-check/{id}` | Check whether a customer ID is a fraudster. Called internally by the Customer service. |

### Notification (internal)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/notification` | Directly persist a notification record. The primary path is via RabbitMQ. |

---

## 🗺️ Roadmap

- [ ] **Migrate to Kubernetes** — replace Docker Compose with Kubernetes manifests (Deployments, Services, ConfigMaps, Secrets) and manage the full stack via `kubectl`. Eureka-based discovery may be replaced with native k8s DNS.
- [ ] Replace stub fraud logic with real rule-based or ML-backed detection.
- [ ] Add Flyway database migrations for each service schema.
- [ ] Add authentication and role-based access control to the API Gateway.
- [ ] Implement a circuit breaker (Resilience4j) on the Customer → Fraud HTTP call.
- [ ] Write integration tests with Testcontainers (PostgreSQL + RabbitMQ).

---

## 🙏 Acknowledgements

- [Spring Boot](https://spring.io/projects/spring-boot) — application framework.
- [Spring Cloud](https://spring.io/projects/spring-cloud) — Eureka, Gateway, and LoadBalancer.
- [Spring AMQP](https://spring.io/projects/spring-amqp) — RabbitMQ integration.
- [Micrometer Tracing + Zipkin](https://micrometer.io/docs/tracing) — distributed tracing.
- [Jib](https://github.com/GoogleContainerTools/jib) — containerisation without a Dockerfile.
- [Lombok](https://projectlombok.org) — boilerplate reduction.

---

## 📄 License

This project is licensed under the [MIT License](./LICENSE). Free for personal and commercial use.
