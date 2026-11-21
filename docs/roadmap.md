# Implementation Roadmap

The PRD describes a four-week delivery target. The schedule below is a sequence of milestones, not permission to skip testing or documentation. If time is limited, reduce optional breadth and keep core behavior verifiable.

## Milestone 1 — Foundation

- Scaffold the Spring Boot application and PostgreSQL development database.
- Define the ER diagram and initial entities.
- Implement registration, login, JWT authentication and refresh-token handling.
- Define Viewer, Planner, Leader and Admin authorization rules.
- Implement trip creation, invite code generation and trip membership.
- Seed one destination's structured venue metadata.
- Add tests as each feature is implemented.

**Exit criteria:** Two users can authenticate, create/join the same trip and access only the actions permitted by their roles.

## Milestone 2 — Collaborative board

- Establish authenticated WebSocket connections scoped to a trip room.
- Implement initial board snapshot delivery and presence.
- Implement itinerary pin, move and lock lifecycle.
- Add optimistic concurrency using `positionVersion`.
- Broadcast board updates and explicit stale-write conflicts.
- Persist chat history and implement reconnect/resynchronization.
- Build scripted clients to exercise multiple concurrent sessions.

**Exit criteria:** At least two scripted clients see consistent board state, and a stale concurrent move is rejected with the latest server state.

## Milestone 3 — Logistics and AI Concierge

- Implement deterministic budget, dietary, hours and proximity filters.
- Add schedule-overlap and budget-breach detection.
- Retrieve relevant venue metadata for a question.
- Generate an answer grounded only in the retrieved candidates.
- Return source venue IDs/metadata with the answer.
- Handle the no-valid-candidate case without fabricating a venue.

**Exit criteria:** Automated tests show that every returned suggestion passes every active hard constraint.

## Milestone 4 — Budget, voting and personalization

- Implement even and weighted expense splits with exact reconciliation.
- Implement contested-slot vote opening, casting, tallying and resolution.
- Record trip interactions.
- Implement a simple hybrid recommendation baseline.
- Add dashboard and session analytics endpoints.
- Add authenticated venue dataset ingestion and validation.

**Exit criteria:** Budget totals reconcile exactly, one vote resolves end-to-end, and recommendations can be evaluated on a held-out generated dataset.

## Milestone 5 — Hardening and evidence

- Add automated tests for board state, concurrency, constraints, budget and RBAC.
- Simulate multiple WebSocket clients and measure sync/fan-out latency.
- Measure API P95 and AI P95 latency using a documented test procedure.
- Verify JWT validation at REST and WebSocket entry points.
- Check secret handling and input validation.
- Complete README, API reference, ER diagram and retrospective.
- Record a reproducible backend demonstration.

## Priority policy

1. Build one end-to-end vertical slice before broadening.
2. Test each feature immediately after implementation.
3. Preserve incremental, descriptive Git commits.
4. Do not claim performance results until measured.
5. Document every deliberate deviation from the requirements.
