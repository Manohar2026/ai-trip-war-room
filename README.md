# AI Trip War Room

A backend-only collaborative trip-planning system built with Java and Spring Boot. It provides shared trip rooms, real-time itinerary synchronization, exact budget calculations, voting, and an AI travel concierge grounded in structured venue data.

> **Project scope:** Backend-only engineering track. REST APIs and WebSocket behavior are demonstrated with Postman and scripted clients; no frontend is required for the initial delivery.

## Technology

- Java 21
- Spring Boot 4.2.0
- Spring WebFlux and Reactor WebSockets
- Spring Data JPA / Hibernate
- PostgreSQL
- Spring Security and JWT (implementation milestone)
- Maven
- JUnit, Mockito and Spring Boot Test
- Docker Compose for local PostgreSQL

## Main capabilities

1. **Trip rooms and access control** — create trips, invite participants and enforce Viewer, Planner, Leader and Admin permissions.
2. **Collaborative itinerary** — pin, move and lock itinerary items with version-based optimistic concurrency.
3. **Real-time collaboration** — board updates, presence, chat, voting and reconnect/resynchronization over WebSockets.
4. **Budget engine** — even and weighted splits with exact per-person reconciliation.
5. **AI Concierge** — deterministic filtering by budget, dietary requirements, operating hours and proximity, followed by retrieval-grounded response generation.
6. **Conflict detection** — detect schedule overlaps and budget-ceiling breaches.
7. **Recommendations and analytics** — a small hybrid recommendation baseline backed by generated interaction data, plus trip-session metrics.

## Repository layout

```text
src/
  main/
    java/com/manohar/aitripwarroom/
      auth/
      analytics/
      budget/
      collaboration/
      common/
      config/
      concierge/
      itinerary/
      recommendation/
      trip/
      user/
      venue/
      voting/
    resources/
      application.yml
  test/
docs/
  architecture.md
  roadmap.md
compose.yaml
pom.xml
```

Each feature package will own its domain model, DTOs, repository, service and controller/handler as needed. The application starts as a modular monolith; modules are not separate microservices.

## Local setup

### Prerequisites

- JDK 21
- Maven 3.6.3+
- Docker Engine with Docker Compose

### 1. Clone the repository

```bash
git clone https://github.com/Manohar2026/ai-trip-war-room.git
cd ai-trip-war-room
```

### 2. Configure local environment

Copy the example file:

```powershell
Copy-Item .env.example .env
```

Edit `.env` and replace `DB_PASSWORD` with a local database password. Generate a unique 32-byte JWT signing secret:

```bash
openssl rand -base64 32
```

Paste the generated value into `JWT_SECRET_BASE64` in `.env`. The placeholders in `.env.example` are deliberately not usable secrets, so the application will not start until you replace them. Never commit `.env` or production secrets.

### 3. Start PostgreSQL

```bash
docker compose up -d postgres
```

### 4. Run the application

\`\`\`bash
mvn spring-boot:run
\`\`\`

Maven Wrapper scripts are not included yet; until they are added, use the installed Maven CLI.

Health endpoint: `http://localhost:8080/actuator/health`

## Development status

The initial foundation is in place: Spring Boot application entry point, PostgreSQL development service, Flyway migration, User/Trip/TripMembership entities and repositories, and unit tests for the first domain behavior. GitHub Actions Maven verification is passing.

Implemented in the current foundation: user registration and login, signed short-lived JWT access tokens, rotating opaque refresh tokens, logout/revocation, and unit tests for authentication behavior. Not implemented yet: trip REST APIs, venue catalog, authenticated room WebSockets, itinerary concurrency, budget calculations, voting, AI Concierge, recommendations and analytics. A capability will not be marked complete until its behavior is implemented and tested.

See [Architecture](docs/architecture.md), [Data Model](docs/data-model.md), [API Reference](docs/api.md) and [Implementation Roadmap](docs/roadmap.md).

## Engineering decisions

- PostgreSQL is the durable source of truth.
- A trip-specific role is stored on the trip membership, because a user can have different permissions in different trips.
- Money uses `BigDecimal`; floating-point arithmetic is not used for allocations.
- Itinerary writes use optimistic concurrency with a monotonically increasing `positionVersion`.
- Hard AI constraints are enforced by deterministic backend code before any LLM-generated explanation.
- JPA is blocking. Database operations must not run on WebFlux event-loop threads.
- The first build is a single-instance modular monolith. Multi-instance coordination is not claimed.

## Security

Do not upload the supplied internal PRD, API keys, JWT secrets, real credentials or private user data to this public repository. Use environment variables for local/runtime secrets.

## License

No license has been added yet.
