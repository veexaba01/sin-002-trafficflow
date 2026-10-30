# TrafficFlow

## Overview

Urban traffic light control, legacy data normalization, and congestion-based routing.

Domain entities: intersections, districts, signal types.

Every class in this repo lives in a single flat package, `co.wethinkcode.trafficflow`. TrafficFlow is built as a set of independent microservices following an evolutionary path from data cleaning through synchronous REST calls to asynchronous MQ decoupling and health alerting:

1. **Clean legacy data** — `IngestionServiceApp` parses and normalizes messy exports from `intersections-legacy.csv`.
2. **Synchronous REST domain wiring** — `IntersectionServiceApp`, `CongestionServiceApp`, and `RoutingServiceApp` call each other directly over HTTP to validate intersections and compute travel times.
3. **Asynchronous event-driven decoupling** — `CongestionServiceApp` broadcasts level changes via an ActiveMQ Topic (`congestion-topic`) so `RoutingServiceApp` updates its local cache without HTTP polling.
4. **Fault monitoring & alerting** — `IntersectionServiceApp` emits periodic heartbeat pulses to an ActiveMQ Queue (`intersection-heartbeat-queue`); `IntersectionWatchdogApp` raises a `503 Service Unavailable` alert if pulses stop.

| Service | Folder | Port | Role |
|---|---|---|---|
| IngestionServiceApp | [ingestion-service/](https://www.google.com/search?q=ingestion-service) | 7020 | Parses and cleans `intersections-legacy.csv` |
| IntersectionServiceApp | [intersection-service/](https://www.google.com/search?q=intersection-service) | 7021 | Canonical source of truth for intersection and district names |
| CongestionServiceApp | [congestion-service/](https://www.google.com/search?q=congestion-service) | 7022 | Tracks the city-wide Congestion Level (0–8) |
| RoutingServiceApp | [routing-service/](https://www.google.com/search?q=routing-service) | 7023 | Provides estimated travel times based on congestion and intersection |
| IntersectionWatchdogApp | [intersection-watchdog/](https://www.google.com/search?q=intersection-watchdog) | 7024 | Raises an alert if the Intersection Service crashes |
| ActiveMQ Classic Broker | [common/](https://www.google.com/search?q=common) | 61616 / 8161 | Shared messaging broker for topics and heartbeat queues |

---

## ActiveMQ Docker Compose Setup

The messaging broker runs in Docker using the official `apache/activemq-classic:5.18.3` container configured in `common/docker-compose.yml`.

### 1. Start the Broker
From the project root:

```bash
cd common
docker compose up -d
```

### 2. Verify Container Health
```bash
docker compose ps
```

* **Client OpenWire Port:** `tcp://localhost:61616` (used by Java JMS clients via `MqConfig.BROKER_URL`)
* **Web Management Console:** `http://localhost:8161` or `http://localhost:8161/admin`
* **Default Credentials:**
  * **Username:** `admin`
  * **Password:** `admin`

### 3. Messaging Channels in the Console
* **Topics Tab (`/admin/topics.jsp`):** View `congestion-topic` to monitor broadcast messages published on congestion updates.
* **Queues Tab (`/admin/queues.jsp`):** View `intersection-heartbeat-queue` to observe 3-second heartbeat pulses enqueuing and dequeuing.

### 4. Stop the Broker
```bash
cd common
docker compose down
```

---

## Build

### Requirements
* **Java:** 17+
* **Maven:** 3.8+
* **Docker:** For the ActiveMQ broker in `common/`

Each service is an **independent** Maven project with no parent POM.

### Build All Modules
From the repository root:

```bash
find . -name pom.xml -execdir mvn clean package -DskipTests \;
```

### Build a Single Module
```bash
cd ingestion-service
mvn clean package
```

---

## Run (Recommended Startup Order)

Start the ActiveMQ broker first so participating services can establish their JMS connections on startup.

### 1. Start Broker
```bash
cd common && docker compose up -d && cd ..
```

### 2. Start Services (in separate terminal tabs or IDE runners)

```bash
# Terminal 1: Ingestion Service (Port 7020)
cd ingestion-service && java -jar target/ingestion-service.jar

# Terminal 2: Intersection Service (Port 7021)
cd intersection-service && java -jar target/intersection-service.jar

# Terminal 3: Congestion Service (Port 7022)
cd congestion-service && java -jar target/congestion-service.jar

# Terminal 4: Routing Service (Port 7023)
cd routing-service && java -jar target/routing-service.jar

# Terminal 5: Intersection Watchdog (Port 7024)
cd intersection-watchdog && java -jar target/intersection-watchdog.jar
```

---

## Automated Unit Tests

Every module includes automated JUnit 5 tests. Run all test suites in one command:

```bash
find . -name pom.xml -execdir mvn test \;
```

Or run tests for a specific module:

```bash
cd ingestion-service && mvn test
```

---

## Verification & API Testing Commands

### Sanity Health Checks
Verify all services are up and listening:

```bash
curl -w "\n" http://localhost:7020/health   # -> OK
curl -w "\n" http://localhost:7021/health   # -> OK
curl -w "\n" http://localhost:7022/health   # -> OK
curl -w "\n" http://localhost:7023/health   # -> OK
curl -w "\n" http://localhost:7024/health   # -> OK
```

### Stage 1: Ingestion & CSV Normalization (Port 7020)
```bash
# List all 17 deduplicated, cleaned records
curl -w "\n" http://localhost:7020/intersections

# Inspect a single intersection
curl -w "\n" http://localhost:7020/intersections/INT-1001

# Verify duplicate collapse (INT-1005 appears only once)
curl -w "\n" http://localhost:7020/intersections/INT-1005

# Unhappy path: unknown intersection ID
curl -i -w "\n" http://localhost:7020/intersections/INT-9999
# -> 404 Not Found
```

### Stage 2: Synchronous REST Domain Services (Ports 7021, 7022, 7023)
```bash
# Validate known intersection ID on source of truth
curl -w "\n" http://localhost:7021/intersections/INT-1001
# -> 200 OK {"id":"INT-1001","valid":true}

# Validate unknown intersection ID
curl -i -w "\n" http://localhost:7021/intersections/INT-9999
# -> 404 Not Found

# Query city-wide congestion level (defaults to 0)
curl -w "\n" http://localhost:7022/congestion
# -> 200 OK {"level":0}

# Base travel time estimate at Level 0: 10.0 + (2.5 * 0) = 10.0 minutes
curl -w "\n" "http://localhost:7023/routes/estimate?from=INT-1001&to=INT-1002"
# -> 200 OK {"from":"INT-1001","to":"INT-1002","congestionLevel":0,"estimatedMinutes":10.0}

# Unhappy path: missing parameter
curl -i -w "\n" "http://localhost:7023/routes/estimate?from=INT-1001"
# -> 400 Bad Request

# Unhappy path: invalid destination ID
curl -i -w "\n" "http://localhost:7023/routes/estimate?from=INT-1001&to=INT-9999"
# -> 404 Not Found
```

### Stage 3: Asynchronous ActiveMQ Topic Decoupling (Ports 7022 & 7023)
```bash
# Update congestion level to 4 (publishes to ActiveMQ congestion-topic)
curl -X POST -w "\n" "http://localhost:7022/congestion?level=4"
# -> 200 OK {"level":4}

# Query Routing Service immediately (instant update via JMS MessageListener, zero HTTP polling)
# Formula: 10.0 + (2.5 * 4) = 20.0 minutes
curl -w "\n" "http://localhost:7023/routes/estimate?from=INT-1001&to=INT-1002"
# -> 200 OK {"from":"INT-1001","to":"INT-1002","congestionLevel":4,"estimatedMinutes":20.0}

# Update congestion level to maximum (Level 8)
# Formula: 10.0 + (2.5 * 8) = 30.0 minutes
curl -X POST -w "\n" "http://localhost:7022/congestion?level=8"
curl -w "\n" "http://localhost:7023/routes/estimate?from=INT-1001&to=INT-1002"
# -> 200 OK {"from":"INT-1001","to":"INT-1002","congestionLevel":8,"estimatedMinutes":30.0}

# Boundary check: reject out-of-bounds congestion level
curl -i -X POST -w "\n" "http://localhost:7022/congestion?level=9"
# -> 400 Bad Request
```

### Stage 4: Watchdog Heartbeat Monitoring & Fault Alerting (Ports 7021 & 7024)
```bash
# 1. Check watchdog status during normal operation
curl -i -w "\n" http://localhost:7024/alert
# -> 200 OK {"status":"HEALTHY","message":"Intersection Service is alive"}

# 2. Simulate service crash:
# Stop intersection-service in Terminal 2 (Ctrl + C)

# 3. Wait 11 seconds (timeout threshold is 10s), then query watchdog:
curl -i -w "\n" http://localhost:7024/alert
# -> 503 Service Unavailable {"status":"ALERT","message":"Intersection Service heartbeat missed"}

# 4. Verify routing-service handles the outage gracefully:
curl -i -w "\n" "http://localhost:7023/routes/estimate?from=INT-1001&to=INT-1002"
# -> 503 Service Unavailable {"error":"Intersection service is unreachable"}

# 5. Restore intersection-service in Terminal 2:
# java -jar target/intersection-service.jar
# Wait 3 seconds for the heartbeat pulse, then query watchdog:
curl -i -w "\n" http://localhost:7024/alert
# -> 200 OK {"status":"HEALTHY","message":"Intersection Service is alive"}
```

---

## Integration Contracts Reference

| From &rarr; To | Stage | Mechanism | Contract / Payload |
|---|---|---|---|
| `ingestion-service` &rarr; `intersection-service` | 1 | REST, `GET` | `GET /intersections` &rarr; `200 OK` + JSON array of cleaned records. |
| `routing-service` &rarr; `intersection-service` | 2 | REST, `GET` | `GET /intersections/{id}` &rarr; `200 OK` `{"id":"...","valid":true}` or `404 Not Found`. |
| `routing-service` &rarr; `congestion-service` | 2 | REST, `GET` | `GET /congestion` &rarr; `200 OK` + `{"level": 0-8}` (fallback before MQ). |
| `congestion-service` &rarr; `routing-service` | 3 | ActiveMQ Topic `congestion-topic` | Published on `POST /congestion`: `{"level": 0-8}`. Subscribed via non-blocking JMS listener. |
| `intersection-service` &rarr; `intersection-watchdog` | 4 | ActiveMQ Queue `intersection-heartbeat-queue` | 3-second periodic heartbeat pulse (`"HEARTBEAT"`). 10-second missed pulse threshold triggers `503 ALERT`. |

## Demo link
https://youtu.be/hOQsbrFX8V0
