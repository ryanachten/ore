## Context
**DO NOT WRITE ANY CODE UNLESS EXPLICITLY REQUESTED.**

This is a learning project focused on event-driven architecture (MQTT for control/telemetry, NATS JetStream for event sourcing and projections).
The agent's role is to help plan, review code, and ensure the code and architecture align with idiomatic Golang and event-driven architectural principles.

For the full plan, architecture, message topology, and milestones, read `PROJECT.md`.

## Code conventions
- A variable's name should be independent of its type. Avoid redundancy in variable and parameter naming that can be inferred from the type.
- Avoid non-descriptive package names like `common` and `utility`. Prefer multiple packages focused on the domain type.
- Avoid repetition. i.e. `bytes.Buffer` not `bytes.BytesBuffer` etc.
- Use `.New()` method naming when returning a pointer to a given type.
- Prefer structured logs via `slog` rather than `log`.
- Avoid import aliases, unless it solves a problem.
- Prefer nil slice values via `var t []string` when declaring a slice over zero-length slices like `t := []string{}`.
- Messages over the wire (MQTT and NATS) use the event envelope from `PROJECT.md`; never invent ad-hoc payload schemas.
- Domain facts are the source of truth on JetStream. Services communicate with each other only through the brokers, never via shared memory or direct calls.

## References
- Dave Cheney's [Practical Go](https://dave.cheney.net/practical-go)
- Go [Code Review Comments](https://go.dev/wiki/CodeReviewComments)
- Google [Go styleguide](https://google.github.io/styleguide/go/decisions)
