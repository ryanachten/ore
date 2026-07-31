# Ore — Task Breakdown

Breakdown of `PROJECT.md` into vertical slices. Each task is small enough to code, PR, and review by a human (target 1–2 hours of implementation). Phases map to the learning goals in `PROJECT.md` §Learning goals; see the index table at the bottom.

## How to read this file

- **Phase** = a milestone oriented around a set of learning outcomes. Phases build in order; within a phase, tasks are ordered by dependency.
- **Task** = a vertical slice: one service (or shared package) + the broker wiring that makes it observable + tests + the compose/Makefile changes it needs. A task is the unit of review — one PR, one human reviewer.
- **Learning** = the concept(s) from `PROJECT.md` §Learning goals this task is designed to exercise.
- **Slice** = the concrete surface area: package/service, topics/streams touched, files.
- **Done when** = acceptance criteria. A task is only done when every bullet is verifiable by the reviewer.

## Definition of done (applies to every task)

- `make lint` and `make test` are green (golangci-lint, `go vet`, unit tests).
- `docker compose up` brings up any services touched; the slice is observable through the brokers (`mosquitto_sub`, `nats` CLI, or the gateway).
- Structured logging via `slog`; no ad-hoc payload schemas — messages use the event envelope from `PROJECT.md`.
- Services communicate only through the brokers, never directly.

---

## Phase 0 — Foundations & the two brokers

Everything later depends on a working compose environment. The envelope and topic names are **not** defined up front — they are born with the first message that needs them (1.1) and grow only as new messages actually appear.

### 0.1 — Repo scaffolding
- **Learning:** — (tooling, carried over from `hazard`)
- **Slice:** Repo root only: `go.mod` (Go 1.26), `Makefile` (`make up/down/logs/lint/test`), `.golangci.yml`, pre-commit config, minimal `README.md` (how to run). No directories yet — `cmd/`, `internal/`, and `web/` come into existence when the first task that needs them lands.
- **Done when:**
  - `make lint` and `make test` run and pass on an empty module.
  - Pre-commit runs golangci-lint on staged Go files.
  - The only directory in the repo is the one holding `.golangci.yml` config (or none). No empty placeholder folders.

### 0.2 — Docker Compose: the two brokers
- **Learning:** — (infra)
- **Slice:** `docker-compose.yml` with `mosquitto` (MQTT, config for QoS/retain, websockets not yet needed) and `nats` (JetStream enabled); healthchecks; `internal/sim/config.go` holding broker addresses/ports and the seed/default tunables from `PROJECT.md` §Catch-22 economy.
- **Done when:**
  - `docker compose up` brings both brokers healthy; `make logs` shows both.
  - `nats stream ls` works against the compose NATS (JetStream up).
  - `mosquitto_pub/sub` round-trip works against compose Mosquitto.

### 0.3 — Connectivity slice: `cmd/poke`
- **Learning:** — (proves the plumbing before any real message)
- **Slice:** `cmd/poke`: publishes a raw message on MQTT and a raw message on JetStream; `cmd/poke` also subscribes to verify the round-trip through compose. Wire into compose.
- **Done when:**
  - Running `poke` against compose logs both publish and receive.
  - No envelope, topics, or event types are invented here — this is a bare broker round-trip; returns non-zero on failure.

---

## Phase 1 — The pulse: world service

Learning outcomes introduced: **Command vs event**, **MQTT QoS semantics** (QoS 0), **Topic & stream design** (in practice), **Schema evolution** (seed). This is where the first real message exists, so this is where the envelope and the first topic constant are born — no more, no less.

### 1.1 — `world` service: tick publisher
- **Learning:** **Command vs event** — `sim/tick` is an event, not a command; nothing commands it. **MQTT QoS semantics** — QoS 0, not retained, fire-and-forget. **Topic & stream design** / **Schema evolution** (seeds) — the first real wire message defines the envelope and the first topic constant.
- **Slice:** `cmd/world`: owns the global clock; publishes `sim/tick` (tick number + timestamp) on MQTT every ~300ms with QoS 0. Alongside it, create `internal/events` (the envelope: version, id, type, tick, timestamp, payload) and `internal/mqtt` (the `sim/tick` topic constant) — scoped to exactly what `sim/tick` needs, nothing else. Owns terrain/seeds only later (see 4.7); for now just the clock. Compose service. Unit tests on the tick counter and envelope round-trip.
- **Done when:**
  - `mosquitto_sub -t sim/tick` shows a monotonic tick every ~300ms.
  - World service does not own entities (no vehicle state anywhere in it).
  - Tick message carries the envelope with a monotonic tick number.
  - `internal/events` and `internal/mqtt` contain only what `sim/tick` needs — no vehicle/base topics, no stream names, no event types for things that don't exist yet.

### 1.2 — `cmd/observer`: first listener
- **Learning:** **Topic & stream design** in practice — receiving real events off the wire; the seed of the ordering lesson (ticks may arrive slightly out of order).
- **Slice:** `cmd/observer`: subscribes to `sim/tick` using the shared `internal/mqtt` constant, logs receipt sequence and any gaps/out-of-order ticks. Dev tool, not a service (no compose service yet).
- **Done when:**
  - Running alongside `world`, it logs each tick with its tick number.
  - It detects and reports gaps or ordering drift — the first observable evidence of distributed ordering.

---

## Phase 2 — Vehicles on the wire

Learning outcomes introduced: **Command vs event** (the same action produces a command *and* a fact), **MQTT QoS semantics** (QoS 1), **Retained messages & LWT**. Vehicles advance one state-machine step per tick (`PROJECT.md` §Tick model, §Entity state machines).

### 2.1 — Vehicle skeleton: tick-driven telemetry
- **Learning:** **Command vs event** — telemetry is an event stream of what happened, published after the fact. **Topic & stream design** — the first vehicle topic enters the tree: `ore/vehicles/<id>/telemetry`.
- **Slice:** `cmd/vehicle` with `--kind=prospector|miner|hauler` and `--id`. Subscribes `sim/tick`, advances an internal clock, publishes telemetry (position, fuel, state) on `ore/vehicles/<id>/telemetry` QoS 0. Extends `internal/mqtt` with the telemetry topic constant (and only that one). Compose runs one demo prospector. Unit tests on tick-advance.
- **Done when:**
  - `mosquitto_sub -t ore/vehicles/+/telemetry` shows telemetry updating per tick.
  - Vehicle has no timer of its own — advancing only on `sim/tick`.
  - Telemetry uses the envelope.

### 2.2 — Retained status + LWT
- **Learning:** **Retained messages & LWT** — vehicle `status` retained so late subscribers see current state; LWT flags dead entities.
- **Slice:** `cmd/vehicle`: publishes retained `ore/vehicles/<id>/status` on state change; sets LWT so an unclean disconnect publishes `status = offline`. Compose/observer shows it.
- **Done when:**
  - Subscribing after a vehicle has been running shows its current retained status.
  - `kill -9` of the vehicle publishes `offline` via LWT (observable by a fresh subscriber).
  - Status transitions mirror the state machine (for now: a minimal `idle` state).

### 2.3 — Commands, QoS 1, and acks
- **Learning:** **MQTT QoS semantics** — QoS 1 for commands: at-least-once with delivery semantics; duplicates possible → an ack channel.
- **Slice:** `cmd/vehicle`: subscribes `ore/vehicles/<id>/commands` (QoS 1), applies command (v1: `goto x,y`), publishes `ore/vehicles/<id>/ack` (command ID + result). Extends `internal/mqtt` with the `commands` and `ack` topic constants. A dev client (`cmd/console`) to send commands.
- **Done when:**
  - Sending `goto` from the console changes the vehicle's target; an ack is observed.
  - A duplicated command message (re-delivery) results in an ack without double-applying (idempotency seed, formalized in 4.5).
  - Command and ack both use the envelope.

### 2.4 — Prospector state machine
- **Learning:** **Tick model** — travel/scan as tick-countdowns (`ceil(dist/speed)`); fuel burn per operating tick; reserve + stall rule.
- **Slice:** `cmd/vehicle --kind=prospector`: `idle → travel → scan → travel → idle`; scan discovers/estimates deposits (emits `deposit.discovered` in 3.1; for now log it). Fuel burn 1/2 ticks, reserve auto-return, `vehicle.stalled` on violation. Unit tests on transitions and fuel math.
- **Done when:**
  - Unit tests cover every transition, tick-countdown travel, fuel burn, and the stall rule.
  - Telemetry/status reflect the current state per tick.

### 2.5 — Miner state machine
- **Learning:** **Tick model** — extract into cargo until full; travel with cargo; unload.
- **Slice:** `cmd/vehicle --kind=miner`: `idle → travel → extract → travel → idle`; cargo capacity; emits `materials.extracted` (logged now, fact in 3.1) at unload into cargo. Fuel 1/tick, reserve + stall rule. Unit tests.
- **Done when:**
  - Unit tests cover extraction fill, capacity, travel, unload, and stall.
  - Same tick-driver contract as the prospector (no own timer).

### 2.6 — Hauler state machine
- **Learning:** **Tick model** — load/travel/unload cycle between miners/deposits and base.
- **Slice:** `cmd/vehicle --kind=hauler`: `idle → travel → load → travel → unload`; unloads into base storage. Fuel 1/tick. Unit tests.
- **Done when:**
  - Unit tests cover load/travel/unload and stall.
  - State machine file is parallel in shape to the prospector/miner ones (reviewable side by side).

---

## Phase 3 — Facts become history

Learning outcomes introduced: **Event sourcing & projections**, **Replay / rebuild**, **Command/query separation** (the gateway is the first read side), **Backpressure & ack policies** (seed).

### 3.1 — Facts to JetStream
- **Learning:** **Event sourcing** — facts are immutable, published to the stream; JetStream log is the source of truth.
- **Slice:** `cmd/vehicle`: publish domain facts (`deposit.discovered`, `materials.extracted`, `vehicle.stalled`, etc.) to `ore.events` via the envelope, with the `tick` metadata. The `ore.events` stream subject constant is added to the shared constants package here, alongside the fact types these vehicles actually emit — no fact types for base events (4.2), the gateway, or anything not yet built. Stream created (init script or `nats` CLI one-liner in docs). Vehicle acts as a dual-plane publisher (MQTT telemetry + JetStream facts) — the *command vs event* distinction made concrete.
- **Done when:**
  - `nats stream view ore.events` shows facts with tick numbers and envelope fields.
  - Same state change produces telemetry (MQTT) *and* a fact (JetStream) — documented as the command/event split.
  - Subject naming matches `PROJECT.md` §Topic & stream design; the constants package holds only what this phase publishes.

### 3.2 — Gateway skeleton + durable consumer
- **Learning:** **Backpressure & ack policies** (seed) — durable consumer, ack after processing, catch-up on reconnect.
- **Slice:** `cmd/gateway`: JetStream durable consumer on `ore.events`; logs facts in order; explicit ack strategy. Compose service.
- **Done when:**
  - Gateway stopped for N facts, restarted → catches up from where it left off (durable consumer).
  - Acks are sent only after successful processing (at-least-once).
  - Consumer config is explicit in code (durable name, ack policy) — the seed for 6.2.

### 3.3 — Ledger projection + replay
- **Learning:** **Replay / rebuild** — append-only ledger; rebuild = wipe + replay from sequence 0.
- **Slice:** `cmd/gateway`: ledger projection (append-only record of every fact, with sequence + tick). `nats` CLI one-liner (docs/) that wipes the projection and replays `ore.events` from seq 0.
- **Done when:**
  - Ledger matches the stream contents after a live run.
  - Wiping and replaying from seq 0 reproduces the identical ledger (asserted in a test).
  - Rebuild one-liner is documented (docs/rebuild.md).

### 3.4 — Inventory projection
- **Learning:** **Event sourcing & projections** — read model rebuilt from facts; **Command/query separation** — reads never touch command paths.
- **Slice:** `cmd/gateway`: inventory read model — base stockpile + per-vehicle cargo + build queue — derived from `materials.extracted`, `materials.deposited`, `fuel.refined`, `vehicle.built`. Test: rebuild reproduces correct numbers from a scripted fact history.
- **Done when:**
  - Projection output matches hand-computed stockpile/cargo from a scripted fact set (golden test).
  - Rebuild reproduces the same state.
  - No read path publishes commands — queries read the projection only.

### 3.5 — Worldmap projection
- **Learning:** **Event sourcing & projections** — the map is derived state, not a service-owned DB.
- **Slice:** `cmd/gateway`: worldmap read model — deposits discovered, vehicle positions, base — from `deposit.discovered` and position/telemetry facts. Test on rebuild.
- **Done when:**
  - Projection reflects deposits/positions derived from facts, matching a scripted history.
  - Rebuild reproduces the map.

---

## Phase 4 — The economy: base service

Learning outcomes introduced: **Outbox pattern**, **At-least-once + idempotency**, **MQTT QoS semantics** (QoS 2), **Command vs event** (base-side). This phase turns the simulation into the catch-22 loop from `PROJECT.md` §Catch-22 economy.

### 4.1 — Base service skeleton
- **Learning:** **Topic & stream design** — base is a first-class actor on its own topics; commands vs events kept separate.
- **Slice:** `cmd/base`: subscribes `sim/tick` + `ore/base/commands`; owns storage seeded from `internal/sim/config.go` (100 iron, 50 copper, 100 fuel); publishes retained `ore/base/status` (stockpile). Extends `internal/mqtt` with the base topics it subscribes/publishes. Compose service.
- **Done when:**
  - Retained base status shows the seed stockpile; updates reflect storage changes.
  - Base has no vehicle state of its own — vehicles remain separate processes.

### 4.2 — Refinery + build queue
- **Learning:** **Command vs event** — commands in (`refine_fuel`, `build_vehicle`), facts out (`fuel.refined`, `vehicle.built`).
- **Slice:** `cmd/base`: refinery recipe (1 iron + 1 copper → 2 fuel) and build queue with vehicle costs (prospector 25/15, miner 40/20, hauler 20/30 per `PROJECT.md` §Vehicles). Responds to commands, emits facts (through the outbox from 4.4). Unit tests on recipes/costs/insufficient-material rejection.
- **Done when:**
  - `refine_fuel` and `build_vehicle` commands produce the documented outcomes or a rejection reason.
  - Rejection (insufficient materials) emits an explicit event — no silent no-ops.
  - Facts use the envelope and carry the tick.

### 4.3 — Dispatch: base → vehicles
- **Learning:** **MQTT QoS semantics** end-to-end — base issues commands to vehicles (QoS 1) the way a player would.
- **Slice:** `cmd/base`: dispatch logic — issues `ore/vehicles/<id>/commands` (QoS 1), watches acks, emits `vehicle.dispatched`. First working loop: dispatch the prospector to scan a seeded deposit, miner extracts, hauler banks.
- **Done when:**
  - A single dispatched prospector completes a scan cycle via commands + acks (integration test or documented manual run).
  - Vehicle acks reconcile with the dispatch expectation; missing ack is handled (retry or logged).

### 4.4 — Outbox pattern
- **Learning:** **Outbox pattern** — base writes facts to a durable outbox before/with the action; a relay publishes to JetStream, so facts survive base crashes. The kafkaesque tie-in.
- **Slice:** `cmd/base`: outbox table (append fact locally, mark delivered); relay publishes to `ore.events`. Test: kill base mid-dispatch, restart → facts are delivered exactly once, no lost facts.
- **Done when:**
  - Facts emitted by base flow through the outbox, not direct publishes.
  - Crash-restart test: no fact is lost and none is duplicated.
  - Outbox state is inspectable (log line or command) for review.

### 4.5 — At-least-once + idempotency
- **Learning:** **At-least-once + idempotency** — duplicate `materials.deposited` must not double-count; dedupe key = envelope ID.
- **Slice:** `cmd/gateway`: dedupe on envelope ID in the inventory projection (and ledger). Test: publish a duplicate `materials.deposited` (same ID) → projection counts it once.
- **Done when:**
  - Replaying a message (or an actual MQTT redelivery) does not double-count deposits.
  - Dedupe is documented: what key, what store, what window.

### 4.6 — QoS 2 for critical ops
- **Learning:** **MQTT QoS semantics** — QoS 2 exactly-once for critical operations (build, dispatch).
- **Slice:** `cmd/base` (publish side) + `cmd/vehicle` (subscribe side): critical commands published QoS 2; document the exactly-once guarantee and its cost. Test/console proves QoS 2 delivery.
- **Done when:**
  - Build/dispatch commands travel QoS 2; routine telemetry stays QoS 0.
  - A QoS 2 command redelivery is handled without double-build (idempotency + QoS 2 interplay documented).

### 4.7 — Full loop integration
- **Learning:** — (system-level; validates the whole economy against `PROJECT.md` §Catch-22 economy)
- **Slice:** All services in compose: world + base + prospector/miner/hauler + gateway. Seed terrain and deposits (world owns terrain/seeds now). Demonstrate bootstrap and fuel starvation.
- **Done when:**
  - The loop runs unattended: prospector finds → miner extracts → hauler banks → stockpile grows or is spent on new vehicles.
  - Fuel starvation is observable (vehicle stalls and auto-returns) and does not deadlock the loop.
  - Tuning knobs all live in `internal/sim/config.go` (no magic numbers in services).

---

## Phase 5 — The glass

Learning outcomes introduced: **Command/query separation** (visible end-to-end), **Ordering & latency** (as a viewing feature).

### 5.1 — Gateway WebSocket server
- **Learning:** **Command/query separation** — one socket, two channels: projection snapshots (queries) + live facts (events).
- **Slice:** `cmd/gateway`: WebSocket endpoint serving projection snapshots (inventory, worldmap) on connect and on change, plus the live fact stream with `tick`. Test with a scripted client.
- **Done when:**
  - On connect a client receives both snapshots; subsequent facts stream live with tick metadata.
  - WS message kinds match `PROJECT.md` §Frontend (BE→FE snapshots + events).

### 5.2 — React scaffold
- **Learning:** — (FE tooling)
- **Slice:** `web/`: Vite + React + TypeScript, WS client with connect/reconnect, dev proxy to gateway.
- **Done when:**
  - App connects to the gateway and logs snapshot + live events (visible in devtools).
  - Reconnect on drop is handled with snapshot re-sync.

### 5.3 — World map view
- **Learning:** **Command/query separation** — the map renders the `worldmap` projection, nothing else.
- **Slice:** `web/`: canvas world map driven by the worldmap snapshot: base, vehicles (by kind), discovered deposits. Update on snapshot change.
- **Done when:**
  - Map reflects worldmap projection state; vehicles move as telemetry-derived facts land.
  - No command path is touched by the renderer.

### 5.4 — Inventory + build/craft queue + command controls
- **Learning:** **Command/query separation** — UI reads projections and writes commands; the two never mix.
- **Slice:** `web/`: inventory panel (stockpile + per-vehicle cargo), build/craft queue; controls that emit `build_vehicle`, `refine_fuel`, `dispatch` → gateway → MQTT. (FE→BE command path from `PROJECT.md` §Frontend.)
- **Done when:**
  - Building/refining/dispatching from the UI produces the corresponding MQTT commands and the resulting facts appear back in the panels.
  - Inventory panel reads the inventory projection; the command path is strictly FE→gateway→MQTT.

### 5.5 — Event ledger + tick scrubber
- **Learning:** **Ordering & latency** — the tick-scrubbing replay makes ordering visible (the deliberate eventual-consistency lesson).
- **Slice:** `web/`: event ledger view (append-only from the ledger projection) + a tick scrubber replaying the live stream.
- **Done when:**
  - Ledger shows each fact with sequence + tick; scrubber rewinds/replays by tick.
  - Out-of-order arrival is visible/flagged in the UI — turning the 6.1 discussion into an observable.

---

## Phase 6 — Hardening

Learning outcomes introduced: **Ordering & latency in distributed systems**, **Backpressure & ack policies**, **Schema evolution**.

### 6.1 — Ordering & latency
- **Learning:** **Ordering & latency in distributed systems** — best-effort determinism, eventual consistency accepted in v1, per-entity ordering.
- **Slice:** Tests + docs: per-entity ordering assertion (facts for one vehicle process in tick order), documented consistency guarantees, `docs/consistency.md` answering "what can a reader observe?"
- **Done when:**
  - A test asserts per-entity ordering holds under normal operation and documents where it can break.
  - `docs/consistency.md` states guarantees, cost, and the deliberate v1 acceptance of eventual consistency.

### 6.2 — Backpressure & ack policies
- **Learning:** **Backpressure & ack policies** — JetStream consumer config (ack policy, max ack pending, max deliveries) and MQTT QoS tuning; observability via slog.
- **Slice:** `cmd/gateway` consumer config + `cmd/vehicle`/`cmd/base` publish settings; slog fields for ack/delivery/redelivery counts. Tests/console demonstrate behaviour under a slow consumer.
- **Done when:**
  - Consumer config is explicit and documented; redelivery/backlog is observable in logs.
  - Slow-consumer behaviour (redelivery, pending) is demonstrated and explained in docs.

### 6.3 — Schema evolution
- **Learning:** **Schema evolution** — version field enforcement, one real migration, and the JSON→protobuf path.
- **Slice:** `internal/events`: enforce `version` on decode; evolve one existing event type with a documented migration (e.g. a v2 payload field with a v1→v2 decoder). Document the protobuf plan.
- **Done when:**
  - v1 events decode after the change; v2 events carry the migration.
  - Unsupported versions fail loudly with a clear error.
  - `docs/schema-evolution.md` covers the versioning contract and the protobuf/registry path.

### 6.4 — Docs pass + runbooks
- **Learning:** — (consolidation)
- **Slice:** `docs/`: dev run, replay/rebuild one-liners, troubleshooting, topic/stream reference. Update `README.md` + `PROJECT.md` pointers if needed.
- **Done when:**
  - A fresh developer can go from clone to running sim in the steps documented.
  - Rebuild, troubleshooting, and topic/stream reference match the code as built.

---

## Phase ↔ Learning-outcome index

| Concept (`PROJECT.md` §Learning goals) | Where it shows up |
|---|---|
| Command vs event | P1.1, P2.1/2.3, P3.1, P4.2 |
| Command/query separation | P3.4/3.5, P5.1/5.3/5.4 |
| Topic & stream design | P1.1/1.2, P2.1, P4.1 |
| MQTT QoS semantics | P1.1 (QoS 0), P2.3 (QoS 1), P4.3, P4.6 (QoS 2) |
| Retained messages & LWT | P2.2 |
| At-least-once + idempotency | P4.5 |
| Event sourcing & projections | P3.1/3.4/3.5 |
| Replay / rebuild | P3.3 |
| Ordering & latency | P6.1, P5.5 (as a view) |
| Backpressure & ack policies | P3.2 (seed), P6.2 |
| Outbox pattern | P4.4 |
| Schema evolution | P1.1 (seed), P6.3 |
