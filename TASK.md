# Ore — Task list (vertical slices)

This is the **source of truth for delivery** (project vision and architecture live in [PROJECT.md](./PROJECT.md)). The work is cut into **small slices**: each slice is a single learning you can write yourself and review in one sitting — a handful of files at most. Slices accumulate into thin end-to-end paths (services → brokers → projections → WebSocket → browser), so the wire grows one chunk at a time and every step yields real learnings.

Horizontal work (infra, messaging, frontend, domain) is deliberately **interleaved**, never front-loaded.

## Working agreement

- **Small enough to write and review yourself.** Each slice is a handful of files and one idea. If a slice starts feeling big, cut scope into its `Deferred` list. Every slice is its own branch/PR; `make up` and `make lint test` stay green.
- **Browser-visible demos accrue.** The first thing in the browser is the pulse (S5). Until then a slice proves itself in `make logs` instead.
- **One primary learning per slice.** If a slice feels like two learnings, it's too fat — cut scope into its `Deferred` list.
- Every slice is its own branch/PR. `make lint test` stays green; the state-machine logic gets unit tests in the same slice.
- New-to-me tech per slice is bounded and called out, so each slice has a specific thing to absorb (Spring + event-driven first).
- The **spine** is: `world` clock → facts (Kinesis) → projections → WS → React, plus the ops plane (EventBridge→SNS→SQS) for commands. It lands in small chunks: the pulse (~S5), the map (~S6), the ops plane (~S7). Once it's live everything after rides it; later slices add one domain feature + one concept each.
- When a slice resolves an entry in PROJECT.md's "Open questions", update it.

## S1 — The skeleton: envelope, LocalStack, first Spring app

**Goal.** `make up` is green with a plain library, the envelope data type everything will ride on, and **one** Spring Boot app that starts clean and is connected to LocalStack. This is the entire Spring surface for now: one app, one AWS client bean, no messaging yet.

**Build**
- Gradle Kotlin DSL multi-module: root + `common` (plain JVM library — **no Spring**); `frontend/` is scaffolded in S5.
- Event envelope in `common`: `{ id, type, tick, source, version, payload }`, `version: 1`. A pure data class for now — JSON mapping lands in S2, where the publish payload gives it something real to map.
- docker compose: LocalStack (auth token); `localstack/init.sh` seeds the `ore-sim` topic. Postgres 16 lands in S8, the slice that first uses it.
- `gateway`: the one Spring Boot app. AWS SDK v2 SNS client wired as a bean with the LocalStack endpoint override; actuator `/health`. (Structured Logback defers to S2, where the tick stream gives it something to format.)
- Makefile: `make up/logs/lint/test`.
- Tests: `version` defaults to 1; payload is immutable. (The round-trip test moves to S2 with the JSON mapping.)
- Verify: `make up` → healthy; `make logs` shows a clean start.

**Primary learning.** LocalStack auth (token in compose); your first Spring Boot app and bean wiring (SDK client with endpoint override). The build scaffold and envelope are just the minimum that makes that app runnable — the schema is born here, but it's a data class and a test, not a learning of its own. The init script seeds the topic the S2 tick will publish to.

**Deferred.** Publish and the envelope's JSON mapping (S2), subscribe (S3), WebSocket (S4), frontend (S5), Postgres, EventBridge, SQS, Kinesis, entities.

**Done when.** `make up` → app is healthy and connected to LocalStack; `make lint test` green (envelope tests pass).

---

## S2 — The clock ticks: publish to SNS

**Goal.** A second Spring Boot app — `world` — publishes `sim.tick` to SNS every 300ms; `make logs` shows a steady tick stream. Fire-and-forget publishing becomes tangible.

**Build**
- `world` module: a second Spring Boot app with a scheduler that publishes `sim.tick` every 300ms (configurable) to the `ore-sim` topic the S1 init script seeded, using the envelope from `common`; the envelope's JSON mapping lands here too (Jackson 3, matching the gateway's managed 3.1.4), with a round-trip test.
- A structured Logback line per tick.

**Primary learning.** SNS publish (fire-and-forget) and a scheduled producer. Bean wiring reuses the S1 pattern, now applied to a second app, so this slice is producer code + a config value.

**Deferred.** SNS subscription (S3), WebSocket (S4), frontend (S5), Postgres, EventBridge, SQS, Kinesis, entities.

**Done when.** `make logs` shows a steady tick stream; `ore-sim` exists (`awslocal sns list-topics`). Nothing is subscribed yet — that's fine.

---

## S3 — The subscription: the wire crosses the broker

**Goal.** `gateway` subscribes to `ore-sim` via an **SNS HTTP endpoint** and now *receives* the ticks `world` publishes. Logs show the full round-trip across two apps: publish out, delivery in.

**Build**
- `gateway` package: an SNS HTTP endpoint (subscription confirmation + delivery handler) that deserializes the delivery body via the `common` JSON mapping and logs each received envelope.
- SNS subscription created in the init script (or via the SDK at startup).

**Primary learning.** SNS subscription delivery over an HTTP endpoint: confirmation, then POST deliveries — and what "fire-and-forget plus at-least-once" feels like from the consumer side.

**Deferred.** WebSocket broadcast (S4), frontend (S5), Postgres, EventBridge, SQS, Kinesis, entities.

**Done when.** `make logs` shows `world` publishing and `gateway` delivering the same `sim.tick` envelopes.

---

## S4 — The broadcast: WebSocket server

**Goal.** A Spring WebSocket server pushes each received envelope to connected clients; a ~10-line client script proves delivery. The wire is now complete from scheduler to WebSocket — only the browser is missing.

**Build**
- Spring WebSocket endpoint (`/ws`): a `WebSocketHandler` that broadcasts envelopes to connected sessions; connect/disconnect log lines.
- Verify: a tiny WS client script connects and prints envelopes as they arrive.

**Primary learning.** Spring WebSocket server: endpoint config, a handler bean, session lifecycle.

**Deferred.** Frontend (S5), Postgres, EventBridge, SQS, Kinesis, entities.

**Done when.** A connected client receives a stream of `sim.tick` envelopes while `make up` runs.

---

## S5 — The Pulse: the browser sees the tick

**Goal.** Open the browser and watch a tick counter ticking every ~300ms. The wire `world → SNS → gateway → WebSocket → React` is complete end-to-end.

**Build**
- `frontend/` scaffold (Vite + React + TS).
- A raw `WebSocket` hook rendering the live tick + last few envelopes.

**Primary learning.** A raw WebSocket client in React — and the first full-stack loop you can watch with your eyes.

**Deferred.** Postgres, EventBridge, SQS, Kinesis, all entity logic.

**Done when.** `make up` → browser shows a ticking counter; `make lint test` green; logs show a steady tick stream.

---

## S6 — The Map: a prospector moves

**Goal.** A canvas shows the base and a prospector advancing one step per tick along a canned out-and-back journey, updating live. A browser that joins mid-run receives the current snapshot.

**Build**
- Kinesis stream `ore-facts` added to init script.
- `vehicle` module: Spring Boot app started with `--kind=prospector`; subscribes to `ore-sim` (SNS HTTP endpoint) for ticks; `Prospector` state machine (idle → travel → … → idle) advances one step per tick; publishes facts (`vehicle.state`, `vehicle.position`) **directly** to Kinesis, partition key `vehicle-<id>`, facts carry `tick`; dedupes duplicate ticks by event id.
- `gateway`: Kinesis shard consumer (poll loop) folds facts into an in-memory world view; broadcasts a **snapshot on WS connect + incremental updates** on each new fact.
- `frontend`: canvas draws base + vehicle (dot, state label); re-renders on snapshot/updates.
- Tests: pure state-machine step tests (no Spring); consumer dedupe test.

**Primary learning.** Event-sourcing seeds (facts are immutable, ordered, replayable records); Kinesis put + consume mechanics (partition key, shard iterator, poll loop); snapshot-vs-live on WS; first entity state machine; running one app with a `--kind` arg.

**Deferred.** Postgres (projections are in-memory here), commands, deposits, fuel, miner, EventBridge.

**Done when.** Two browsers; the one that connects mid-run still shows the same live map; gateway logs show facts streaming from Kinesis.

---

## S7 — The Ops Plane: commands & telemetry

**Goal.** Click the map to dispatch the prospector to that point — the command path `gateway → EventBridge → SNS → SQS → vehicle` works, idempotently. The vehicle's status/telemetry streams back over `EventBridge → SNS → gateway → WS`, and a broker inspector shows message counts. Command vs event becomes tangible.

**Build**
- EventBridge rules: `route-commands` (`command.*` → SNS `ore-commands`), `route-telemetry` (`telemetry.*` → SNS `ore-telemetry`).
- SNS `ore-commands` → SQS `ore-vehicle-1` (+ DLQ) with a **filter policy** (`vehicleId`); SNS `ore-telemetry` → gateway (SNS HTTP endpoint).
- `gateway`: `POST /api/commands` → `command.dispatch` → EventBridge; plus WS live-feed pane for telemetry; broker inspector (SNS topics, SQS depth via `getQueueAttributes`).
- `vehicle`: SQS consumer loop (long-poll) with an **idempotent handler** (dedupe by event id); emits `vehicle.dispatched` fact and `telemetry.status`.
- `frontend`: click-to-dispatch (disabled while in flight), live event feed, inspector panel.
- Tests: delivering the same command twice causes one action; a command for another vehicle is filtered out and never lands.

**Primary learning.** Command vs event (the same action produces a command on the ops plane and a fact on Kinesis); EventBridge content-based routing; SNS→SQS filter policies; SQS at-least-once + visibility timeout + idempotency; DLQ present (redrive deferred).

**Deferred.** Runtime per-entity queue provisioning (S11), DLQ redrive tooling, deposits, fuel.

**Done when.** Click-to-dispatch works from the browser; a replayed duplicate command doesn't double-travel; inspector shows queue depth after commands; status events stream live.

---

## S8 — The Source of Truth: projections & replay

**Goal.** The gateway now persists `worldmap`, `inventory`, and `ledger` projections in Postgres from the fact stream; snapshot-on-join reads Postgres; `make replay` wipes the DB and rebuilds everything from Kinesis. The fact log is provably the source of truth.

**Build**
- Postgres 16 container added to docker compose; now used: Flyway in `gateway`; migrations create `worldmap`, `inventory`, `ledger`; Kinesis consumer writes projections via `JdbcTemplate` (in-memory view deleted).
- Consumer checkpointing (processed sequence per shard) so replay is explicit.
- `make replay`: truncate projection tables, drop checkpoints, re-consume from the trim horizon.
- Tests: projection upsert; replay over existing data converges (idempotent).

**Primary learning.** Projections as disposable read models vs the log as truth; replay/rebuild; Flyway + `JdbcTemplate`; Spring `DataSource` + write transactions.

**Deferred.** Outbox (S11), deposits, miner, fuel, ledger UI.

**Done when.** `make replay` rebuilds a wiped DB into the same map; snapshots come from Postgres; all three projections populated.

---

## S9 — The Discovery: deposits & world reactions

**Goal.** Dispatch the prospector to scan; on completion a deposit node appears on the map with a richness estimate. `world` is now a first-class participant that *reacts to facts*.

**Build**
- `world`: **a stateful service from here on** — it gains Postgres + a Kinesis consumer (until now it was just a tick clock); Flyway + Postgres `deposits`; seeds the terrain deterministically at startup; emits `deposit.seeded` facts (direct put for now — outbox is S11).
- `prospector`: scan step is a tick-countdown; emits `scan.completed` (location + radius).
- `world` becomes a second Kinesis consumer: reacts to `scan.completed` → emits `deposit.discovered` (deposits within radius, with estimates).
- `gateway`: `deposits` projection; map renders deposit markers (colored by richness).
- `frontend`: deposit markers, "scanning" indicator.
- Envelope note: adding new fact `type`s is the first real **schema evolution** — keep payload parsing tolerant.

**Primary learning.** Event-driven collaboration between services (world reacts to a fact a vehicle published); multiple independent consumers of one stream (world + gateway each own their iterator); a service growing state + responsibilities; schema evolution.

**Deferred.** Extraction, fuel, base, outbox.

**Done when.** Scan → deposit appears with estimate; same seed → same deposits across runs.

---

## S10 — The Mine: extraction, cargo & inventory

**Goal.** A miner extracts ore from a discovered deposit: travels, fills cargo over N ticks, returns to base and unloads. An inventory panel shows stockpile + per-vehicle cargo, derived from facts. Two vehicle kinds run side by side.

**Build**
- `miner` kind: idle → travel → extract → travel → idle; cargo capacity, extraction rate; unload at base emits `materials.deposited`.
- Facts: `vehicle.state`, `materials.extracted` (cargo), `materials.deposited` (stockpile).
- `gateway`: `inventory` projection becomes real — stockpile + per-vehicle cargo, pushed on change.
- `vehicle-2` queue + DLQ + filter policy (still statically provisioned; runtime provisioning is S11).
- `frontend`: inventory panel (stockpile + cargo bars); select a miner → dispatch to a deposit (reuses the S7 command path).
- `:common:sim`: mining rates, cargo sizes, distances (tunables).
- Tests: miner state machine incl. cargo-full → auto-return.

**Primary learning.** A second entity state machine; derived accounting (stockpile = a fold over facts); economy enters the sim; scaling the command path to a second filtered queue.

**Deferred.** Base service (storage becomes authoritative there), fuel economy, refinery, build queue, outbox, hauler.

**Done when.** Dispatch miner → ore appears in cargo → unloads at base → inventory panel updates.

---

## S11 — The Base: the catch-22 closes

**Goal.** The base service is live: it owns storage, the refinery, the build queue, refuel, and vehicle provisioning. Spend the stockpile to build a second miner → it appears on the map with its own queue and subscription, ready to dispatch. Fuel burns per tick; low fuel → auto-return and refill from the stockpile. This is the whole loop.

**Build**
- `base` service: Postgres (state + **outbox** table), Flyway, SQS `ore-base` + DLQ; consumes `command.build_vehicle`, `command.refine_fuel`, `command.dispatch`.
- **Transactional outbox**: state change + fact row in one transaction; a poller publishes outbox rows to Kinesis. The reliable-facts pattern, contrasted with the vehicles' direct puts.
- Refinery (`1 iron + 1 copper → 2 fuel` → `fuel.refined`); build queue with timers; on completion deduct cost, emit `vehicle.built`, and **provision the new vehicle's SQS queue + SNS subscription at runtime via the SDK** — the flagship provisioning learning.
- Fuel economy: burn per tick, reserve + auto-return (`vehicle.stalled`), refill from stockpile at zero cost.
- `gateway`: `inventory` becomes base-authoritative (from base's facts).
- `frontend`: build panel (kind + cost), refine button, build queue display.
- Tests: outbox survives a poller outage (fact not lost); build deducts cost + provisions the queue; build deduped by command id.

**Primary learning.** Transactional outbox; runtime resource provisioning via the SDK; a stateful service vs the stateless tick machines; the catch-22 economy (finite stockpile, everything burns fuel).

**Deferred.** Hauler, archive, ledger UI, DLQ redrive.

**Done when.** From a fresh seed you can refine fuel, build a second miner (its queue appears in the inspector), dispatch it, and watch the stockpile drain and refill. Run the stockpile down and the sim visibly stalls until you build.

---

## S12 — The Archive: facts land in S3

**Goal.** Every fact lands in S3 `ore-ledger`, date-partitioned, buffered through Firehose. Cold storage is real.

**Build**
- Firehose delivery stream `ore-facts-delivery` (Kinesis source) → S3 `ore-ledger` in init script; buffer settings tuned down so it's observable quickly.
- `make archive` lists the date-partitioned objects; `make archive-peek` dumps one partition's raw facts.
- Note in the ledger view where S3 (full archive) vs the Postgres ledger (recent facts) differ.

**Primary learning.** Buffering / cold path (Firehose micro-batch → S3); partition layout; hot (Postgres) vs warm (Kinesis) vs cold (S3) planes.

**Deferred.** Athena-style queries, lifecycle policies.

**Done when.** After a minute of sim, `make archive` lists dated objects containing JSON facts.

---

## S13 — The Frontend: ledger, scrubber, polish

**Goal.** The app becomes the whole experience: a ledger view with a tick scrubber, a complete inspector (incl. DLQ counts and consumer lag), polished map/inventory/build panels. Everything rides the existing spine.

**Build**
- Ledger view from the `ledger` projection (gateway REST/WS); tick scrubber pauses and steps through captured facts (historical replay across restarts = stretch).
- Inspector completes: SNS topics, SQS queues (depth + DLQ), Kinesis shard iterator age.
- Empty/loading/error states; README screenshots.
- Tests: FE-adjacent (command validation, rendering states) as far as makes sense.

**Primary learning.** Command/query separation end-to-end (reads via projections, writes via the ops plane); latency perception (live push vs scrubber replay).

**Deferred.** The stretch list below.

**Done when.** You can watch the whole loop from the ledger, scrub ticks, inspect every broker, and the sim runs unattended.

---

## S14 — Depth (stretch)

Each item is one focused learning; pick up as desired. "Appears in" maps to PROJECT.md's learning table.

- **Hauler** — load/unload handoff between miner ↔ hauler ↔ base: inter-entity event choreography. *Concept: event choreography, orchestration-vs-choreography.*
- **DLQ redrive** — `make redrive` tooling to drain DLQs back onto the queue. *Concept: dead-letter handling.*
- **SQS FIFO ordering** — per-entity command ordering via FIFO queues. *Concept: ordering.*
- **EventBridge Scheduler as the clock** — replaces the SNS `sim.tick`. *Concept: command vs event (a scheduled command).*
- **Consumer-lag metrics** — shard iterator age surfaced in the inspector. *Concept: backpressure & throttling.*
- **Avro / schema registry** — versioned binary envelopes. *Concept: schema evolution.*
- **Terraform provisioning** — replace the static init script. *Concept: runtime provisioning.*
