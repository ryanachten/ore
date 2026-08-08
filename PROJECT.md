# Ore

A distributed mining and resource-utilisation simulation built to learn **event-driven architecture** hands-on.

Raw materials are prospected in the landscape, extracted, and hauled back to the base where they are stored and spent to build new vehicles and mining equipment. The base is seeded with a small stockpile and one of each vehicle — **materials are required to extract materials** — so the player must bootstrap capability before the stockpile runs out.

The simulation is an evolution of [hazard](https://github.com/ryancdotnet/hazard): the same tick-based entity model, but the entities are now *separate Spring Boot processes* that communicate only through AWS messaging services emulated by LocalStack.

## Learning goals

The point of this project is to get hands-on with event-driven architecture on AWS services. Each milestone maps to specific concepts:

| Concept | Where it shows up |
|---|---|
| Command vs event | Commands ride EventBridge→SNS→SQS; facts are immutable records on Kinesis — the same action produces both |
| Command/query separation | Commands via SQS; reads from Postgres projections |
| Event bus & content-based routing | EventBridge rules route operational events by type/pattern |
| Pub/sub fan-out + filter policies | SNS topics fan telemetry to the gateway; SNS filter policies route commands to per-entity SQS queues |
| Delivery semantics | SQS at-least-once + visibility timeout + DLQ (≈ MQTT QoS 1); SNS fire-and-forget (≈ QoS 0); Kinesis ordered per partition |
| Idempotency | Dedupe by event id + tick — SQS/Kinesis duplicates must not double-count |
| Event sourcing & projections | Kinesis `ore-facts` is the source of truth; Postgres read models are rebuilt from it |
| Replay / rebuild | Truncate projections, replay Kinesis from the trim horizon |
| Ordering & latency | Per-partition ordering; tick-number metadata; eventual consistency accepted in v1 |
| Backpressure & throttling | Kinesis shard iterator age / consumer lag, SQS batch sizes |
| Transactional outbox | Base writes state + fact in one Postgres transaction; a poller publishes to Kinesis |
| Dead-letter handling | Per-queue DLQ + redrive tooling |
| Archive / cold path | Firehose buffers Kinesis facts into the S3 ledger, partitioned by date |
| Schema evolution | Enveloped events with a `version` field; JSON in v1 |
| Runtime resource provisioning | Building a vehicle provisions its SQS queue + SNS subscription via the SDK |

## Tech stack

- **Backend**: Java 21, Spring Boot 3.4, Gradle (Kotlin DSL) multi-module — one runnable app per service
- **AWS access**: `software.amazon.awssdk` v2 clients (kinesis, sns, sqs, eventbridge, firehose, s3) wired as plain Spring beans with a LocalStack endpoint override — no Spring Cloud Stream, so delivery semantics stay visible
- **Infra**: LocalStack (single image; requires a free account auth token), Postgres 16, docker compose
- **DB**: Flyway migrations + `JdbcTemplate` (mechanics stay visible; no JPA magic)
- **Frontend**: React + TypeScript + Vite, raw WebSocket to the gateway
- **Conventions**: structured logging via Logback, per-module unit tests, `make` targets

## Architecture

Two planes, five AWS services:

- **Facts plane (Kinesis)** = the event-sourcing log. Every state change is an immutable fact published to `ore-facts`, partition key = aggregate id (base, `vehicle-<id>`). Ordered, replayable. This is the source of truth.
- **Ops plane (EventBridge → SNS → SQS)** = the living channel. Services publish operational events to the EventBridge bus; rules route telemetry → SNS → gateway (→ WebSocket → FE) and commands → SNS → per-entity SQS queues (filter-policy scoped).
- **Archive plane (Firehose → S3)** = the cold ledger. Firehose consumes the Kinesis stream and lands raw facts in `ore-ledger`, date-partitioned.

```mermaid
graph TB
    subgraph Ops["Control / telemetry plane (EventBridge + SNS + SQS)"]
        EB[EventBridge bus]
        ST[SNS ore-sim]
        TT[SNS ore-telemetry]
        CT[SNS ore-commands]
        QV[SQS ore-vehicle-&lt;id&gt; + DLQ]
        QB[SQS ore-base + DLQ]
        EB -.rule telemetry.*.-> TT
        EB -.rule command.*.-> CT
        CT -.filter policy per entity.-> QV
        CT -.-> QB
    end
    subgraph Facts["Facts / event-sourcing plane (Kinesis)"]
        K[(Kinesis ore-facts)]
        FH[Firehose ore-facts-delivery]
        S3[(S3 ore-ledger)]
        K --> FH --> S3
    end
    subgraph Fe["Frontend"]
        React[React app<br/>map · inventory · build queue · ledger]
    end
    World[world service] --sim.tick--> ST
    Vehicle[vehicle service] --telemetry / status--> EB
    Vehicle --facts--> K
    Base[base service] --facts via outbox--> K
    QV --> Vehicle
    QB --> Base
    GW[gateway service] -.shard consumer.- K
    TT --> GW
    GW <--> React
    React --commands--> GW
    GW --> EB
```

Services never talk directly — only through the brokers.

## Resource inventory

| Resource | Name | Role |
|---|---|---|
| EventBridge bus | default | services publish all operational events |
| EventBridge rule | `route-telemetry` | `telemetry.*` / `status.*` → SNS `ore-telemetry` |
| EventBridge rule | `route-commands` | `command.*` → SNS `ore-commands` |
| SNS topic | `ore-sim` | world publishes `sim.tick` (fire-and-forget) |
| SNS topic | `ore-telemetry` | fan-out to gateway (WS → FE) |
| SNS topic | `ore-commands` | fan-out to per-entity SQS via filter policies |
| SQS queue | `ore-base` (+ DLQ) | base commands: build, refine, dispatch |
| SQS queue | `ore-vehicle-<id>` (+ DLQ) | per-vehicle commands; provisioned on build |
| Kinesis stream | `ore-facts` | ordered fact log, partition key = aggregate id |
| Firehose | `ore-facts-delivery` | Kinesis source → S3 |
| S3 bucket | `ore-ledger` | raw fact archive, partitioned by date |

Provisioned by a LocalStack init script at startup; per-vehicle queues are provisioned at runtime by `base` when a vehicle is built.

## Services

| Service | Responsibility |
|---|---|
| `world` | Owns the global clock: publishes `sim/tick` to SNS `ore-sim` every ~300ms. Owns terrain and seeds deposits (Postgres). Does **not** own entities. |
| `base` | Material storage, build queue, refinery, vehicle dispatch. Postgres state + transactional outbox → Kinesis facts. Consumes `ore-base` queue. Provisions a vehicle's queue + subscription when it is built. |
| `vehicle` | One app, `--kind=prospector\|miner\|hauler`. A state machine that advances one step per tick. Consumes its own SQS queue; publishes telemetry to EventBridge and facts to Kinesis. |
| `gateway` | Kinesis shard consumer → Postgres projections; SNS telemetry consumer → WebSocket push; turns FE commands into EventBridge events; serves snapshots and the ledger. |

## Tick model

- The `world` service owns the clock. It broadcasts `sim.tick` on SNS `ore-sim` (fire-and-forget, not persisted).
- Entities do **not** run their own sim timers. On each tick they advance their state machine by exactly one step.
- Travel, scanning and extraction are modelled as **tick-countdowns** (e.g. travel to `(x,y)` takes `ceil(dist / speed)` ticks).
- Fuel is consumed per operating tick.
- Facts carry the `tick` number they occurred on, so downstream can reason about ordering.
- SNS is at-least-once, so ticks may duplicate or arrive slightly out of order. Consumers track last-processed tick and dedupe by event id.
- Determinism is **best-effort**; v1 accepts eventual consistency — an intentional, documented learning point about ordering in distributed systems.

## Projections (Postgres)

| Projection | Read model | Drives |
|---|---|---|
| `inventory` | base stockpile + per-vehicle cargo + build queue | FE inventory, crafting gate, economy balance |
| `worldmap` | deposits discovered, vehicle positions, base | FE map render |
| `ledger` | recent facts | FE history, replay, debugging (S3 is the full archive) |

All projections are **rebuildable**: wipe the tables, replay Kinesis from the trim horizon (`make replay`).

## Domain model

### Materials (v1)

- **Iron Ore** — mined; building material
- **Copper Ore** — mined; building material
- **Fuel** — refined at base from ore; consumed by every vehicle per operating tick

Materials are simultaneously *currency* (build vehicles) and *consumable* (burn fuel), so every unit spent is a choice.

### Vehicles

| Vehicle | Behaviour | Cost (defaults) |
|---|---|---|
| **Prospector** | Travels to a scan site, scans a radius, discovers/estimates deposits. Speed fast, scan expensive. | 25 iron + 15 copper |
| **Miner** | Travels to a known deposit, extracts ore into cargo until full, returns to base. | 40 iron + 20 copper |
| **Hauler** | Ferries ore between miners/deposits and base, unloads into storage. | 20 iron + 30 copper |

Fuel burn (defaults, tune later): prospector 1 / 2 ticks, miner 1 / tick, hauler 1 / tick. Vehicles keep a reserve to return to base; when fuel runs low they auto-return to refuel (base refills from stockpile at zero cost) — the soft pressure that forces spending.

### Catch-22 economy (seed + defaults)

Base starts with: **1 prospector, 1 miner, 1 hauler** and stockpile **100 iron, 50 copper, 100 fuel**.

Refinery recipe: `1 iron + 1 copper → 2 fuel`.

The loop: prospectors find deposits → miners extract → haulers bank the materials → spend on new vehicles and fuel → more capacity → faster extraction. The tension is early: the stockpile is finite and everything burns fuel, so a player who dispatches unwisely stalls. Numbers are tunable in a single config (`:common:sim`).

## Entity state machines

Each vehicle is a state machine advancing per tick:

- **Prospector**: `idle → travel → scan → travel → idle` (emits `deposit.discovered` at scan completion)
- **Miner**: `idle → travel → extract → travel → idle` (emits `materials.extracted` at unload into cargo; `materials.deposited` when the hauler/base receives it)
- **Hauler**: `idle → travel → load → travel → unload` (emits `materials.deposited` at base)
- **Base**: reacts to ticks and commands; owns the build queue and refinery; emits `vehicle.built`, `fuel.refined`, `vehicle.dispatched`; persists state to an outbox for reliable fact emission

Stall rule: an operation that would take fuel below reserve halts the vehicle (`vehicle.stalled`), and it returns to base to refuel.

## Outbox pattern

`base` and `world` are stateful (Postgres) and emit facts reliably via a **transactional outbox**: the state change and the fact are written in one transaction, and a poller publishes outbox rows to Kinesis. Vehicles are stateless tick machines and publish facts directly (at-least-once; consumers dedupe by event id + tick). Both sides of the reliable-messaging question are exercised.

## Frontend (React)

Raw WebSocket connection to the gateway. Message kinds:

- **BE → FE**: projection snapshots (inventory, worldmap) on connect + on change; live event stream (each fact with its `tick`, enabling a tick scrubber for replay)
- **FE → BE**: commands — `build_vehicle`, `refine_fuel`, `dispatch` (select entity + destination)

Views (v1): canvas world map (entities, discovered deposits, base), inventory panel (stockpile + per-vehicle cargo), build/craft queue, event ledger, and a small "broker inspector" showing live messages per SNS topic / SQS queue / Kinesis shard.

## Delivery

Delivery is tracked in [TASK.md](./TASK.md) — a vertical-slice breakdown of this project. Each slice delivers a complete end-to-end path visible in the browser and maps to specific learning concepts in the table above.

## Open questions / future work

- Tick routing through SNS vs EventBridge Scheduler as the clock.
- Ledger reads from Postgres vs S3-only (Athena-style queries).
- Split the Kinesis consumer out of the gateway into a dedicated `projector` service.
- Protobuf / Avro event encoding and schema-registry tooling.
- Distributed tick ordering: sequence-number reconciliation, or per-entity clocks — a deliberate later exercise.
- Refuel delivery (fuel tanker entity) instead of auto-return.
- Continuous terrain vs grid; pathfinding upgrades.
- Terraform (or `awslocal`) as the provisioning mechanism for the static resources vs the LocalStack init script.
