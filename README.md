# Device Management API
A RESTful API built with **Spring Boot 3** for managing device resources.  
The system supports full lifecycle operations including creation, updates, filtering, and deletion — with strict business rule enforcement.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Build tool | Gradle 8.x |
| Framework | Spring Boot 3.4.x |
| Web | Spring Web (MVC) |
| Persistence | Spring Data JPA + Hibernate |
| Database (prod) | PostgreSQL 16 |
| Database (dev/test) | H2 (in-memory / file-based) |
| Migrations | Flyway |
| Validation | Jakarta Bean Validation |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Testing | JUnit 5 + Mockito |
| Coverage | JaCoCo |
| Containerisation | Docker + Docker Compose |

---

## Domain Model

A Device contains:

| Field | Type | Notes |
|---|---|---|
| `id` | Long | Auto-generated, immutable |
| `name` | String | Required |
| `brand` | String | Required |
| `state` | Enum | `AVAILABLE`, `IN_USE`, `INACTIVE` |
| `createdAt` | LocalDateTime | System-generated, immutable |

---

## Business Rules

- `createdAt` is set by the system on creation and **cannot be updated**
- `name` and `brand` **cannot be updated** if the device is `IN_USE`
- Devices in `IN_USE` state **cannot be deleted**
- `brand` and `state` query filters are **mutually exclusive** — only one may be used per request

---
## API Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/device` | Create a new device |
| `GET` | `/api/v1/device` | Get all devices |
| `GET` | `/api/v1/device?brand=Apple` | Filter devices by brand |
| `GET` | `/api/v1/device?state=AVAILABLE` | Filter devices by state |
| `GET` | `/api/v1/device/{id}` | Get device by ID |
| `PUT` | `/api/v1/device/{id}` | Fully update a device |
| `PATCH` | `/api/v1/device/{id}` | Partially update a device |
| `DELETE` | `/api/v1/device/{id}` | Delete a device |

### Example Request Body (POST / PUT)

```json
{
  "name": "iPhone 15",
  "brand": "Apple",
  "state": "AVAILABLE"
}
```

### Example PATCH Body (partial fields only)

```json
{
  "state": "IN_USE"
}
```

---

## Project Profiles

The application uses three Spring profiles:

| Profile     | Purpose | Database |
|-------------|---|---|
| *(default)* | Production / Docker | PostgreSQL (via env vars) |
| `dev`       | Local development | H2 file-based (`./data/devicedb`) |
| `test`         | Automated tests | H2 in-memory |

---

## Running Locally

### Prerequisites

- Java 21+
- Gradle 8+ (or use the included wrapper `./gradlew`)

### Option A — Local run with H2 (dev profile, no PostgreSQL needed)

This is the easiest way to run locally without Docker or a PostgreSQL installation.

```bash
  ./gradlew bootRun --args='--spring.profiles.active=dev'
```

The app starts on **http://localhost:8080** and stores data in `./data/devicedb.mv.db`.

### H2 Console (dev / test profiles)

When running with `--spring.profiles.active=test`, the H2 console is available at:

**http://localhost:8080/h2-console**

Use these exact credentials:

| Field | Value |
|---|---|
| Driver Class | `org.h2.Driver` |
| JDBC URL | `jdbc:h2:file:./data/devicedb` |
| User Name | `sa` |
| Password | `password` |

> ⚠️ **Important:** The JDBC URL must match exactly what is in `application-dev.properties`. If the console shows a different URL by default, clear it and type `jdbc:h2:file:./data/devicedb` manually. Do **not** use `jdbc:h2:~/test` or any other default — it will connect to a different empty database.

> ⚠️ The H2 console is **disabled** in the default (production) profile. It is only available when running with `dev` or `test` profiles.

### Option B — Local run with PostgreSQL (default profile)

1. Start a local PostgreSQL instance (or use Docker for just the DB):

```bash
  docker run -d \
    --name postgres-local \
    -e POSTGRES_DB=devicedb \
    -e POSTGRES_USER=sa \
    -e POSTGRES_PASSWORD=password \
    -p 5432:5432 \
    postgres:16-alpine
```

2. Run the application:

```bash
  ./gradlew bootRun
```

The app will connect to `jdbc:postgresql://localhost:5432/devicedb` using the defaults defined in `application.properties`. Flyway will run the migrations automatically on startup.

---

## Running with Docker Compose (Recommended for full stack)

This is the recommended approach — it starts both PostgreSQL and the application together.

### Step 1 — Build and start

```bash
  docker compose up --build
```

This will:
1. Build the Spring Boot application image
2. Start PostgreSQL with a persistent named volume (`pgdata`)
3. Wait for PostgreSQL to be healthy before starting the app
4. Run Flyway migrations automatically on startup

The app is available at **http://localhost:8080**

### Step 2 — Stop

```bash
  docker compose down
```

To also remove the database volume (destroys all data):

```bash
  docker compose down -v
```

### Using a custom DB password

```bash
  DB_PASSWORD=mysecurepassword docker compose up --build
```

Or create a `.env` file in the project root:

```env
DB_PASSWORD=mysecurepassword
```

### Rancher Desktop users

If you use Rancher Desktop instead of Docker Desktop:

1. In Rancher Desktop → **Preferences** → **Container Engine** → select **`dockerd (moby)`**
2. All `docker` and `docker compose` commands above work identically
3. On **Windows**, ensure your project folder is under `C:\Users\` so volume mounts are allowed

---

## Building the Docker image manually

```bash
  # Build image
  docker build -t device-management .

  # Run with PostgreSQL (requires a running PostgreSQL instance)
  docker run -d \
    -p 8080:8080 \
    -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/devicedb \
    -e SPRING_DATASOURCE_USERNAME=sa \
    -e DB_PASSWORD=password \
    --name device-management-container \
    device-management:latest
```

> On Linux, replace `host.docker.internal` with your actual host IP or use `--network host`.

```bash
  # Run with H2
  docker run -d \
    -p 8080:8080 \
    -v $(pwd)/data:/data \
    -v $(pwd)/logs:/logs \
    -e DB_PASSWORD=your_secure_db_pass \
    -e ADMIN_PASSWORD=your_api_pass \
    --name device-management-container \
    device-management:latest
```

> If you are on Windows, ensure the path `$(pwd)/data` is within a directory that Rancher Desktop has permission to share (usually your `C:\Users` folder).

---

## Running Tests

```bash
  ./gradlew test
```

Tests run against the `dev` profile (H2 in-memory) automatically. No external database is required.

To run tests and generate all reports at once:

```bash
  ./gradlew clean test jacocoTestReport
```

---

## Reports

After running `./gradlew clean test jacocoTestReport`, the following reports are generated locally:

| Report | Path |
|---|---|
| **Test results** (HTML) | `build/reports/tests/test/index.html` |
| **JaCoCo coverage** (HTML) | `build/reports/jacoco/test/html/index.html` |
| **Build problems** | `build/reports/problems/problems-report.html` |

Open any of them directly in your browser:

```bash
  # macOS
  open build/reports/tests/test/index.html
  open build/reports/jacoco/test/html/index.html

  # Linux
  xdg-open build/reports/tests/test/index.html

  # Windows (PowerShell)
  start build/reports/tests/test/index.html
```

---

## API Documentation (Swagger UI)

Once the application is running:

| Resource | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |

---

## Exception Handling

All errors are returned as a structured `ApiError` JSON body:

```json
{
  "code": "DEVICE_IN_USE",
  "message": "Device 3 cannot be modified while IN_USE",
  "status": 409,
  "path": "/api/v1/device/3",
  "timestamp": "2025-01-01T12:00:00"
}
```

| HTTP Status | Error Code | Trigger |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Missing/invalid request fields |
| 400 | `INVALID_STATE` | Unknown enum value for state |
| 400 | `FILTER_CONFLICT` | Both `brand` and `state` filters used together |
| 404 | `DEVICE_NOT_FOUND` | Device ID does not exist |
| 409 | `DEVICE_IN_USE` | Update/delete blocked by business rule |
| 500 | `INTERNAL_ERROR` | Unexpected server error |

---

## Key Design Decisions

- **DTO-based API** — entities are never exposed directly; `DeviceRequest` / `DeviceResponse` separate API contract from persistence model
- **Typed exception hierarchy** — `DeviceException` base class with `ErrorCode` enum avoids stringly-typed error handling
- **`@Transactional(readOnly = true)`** on the service class by default; write methods override with `@Transactional`
- **Flyway migrations** — schema is version-controlled in `src/main/resources/db/migration/`; `ddl-auto=validate` in production ensures no silent schema drift
- **Optimistic locking** — `@Version` field on `Device` prevents lost updates under concurrent modification

---

## Future Improvements

- Add authentication (Spring Security + JWT)
- Add pagination and sorting to `GET /api/v1/device`
- Add caching layer (Redis) for read-heavy queries
- Add audit logging (created by / updated by)
- Event-driven notifications via Kafka on state changes
- Metrics and health endpoint hardening (Spring Actuator)

---

## Author

Device Management - built for technical assessment and production readiness demonstration.