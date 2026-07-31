# Decisions

Architecture decisions for the project, recorded as they're made. Each entry captures the context, the choice, and the reasoning so a later reader (or a future reversal) knows why things are the way they are.

## Keep Mosquitto as the MQTT broker

**Status:** Accepted (rejected alternative, revisit later)

**Context:** NATS Server ships a built-in MQTT gateway (MQTT 3.1.1) that since v2.10 supports QoS 0, 1 and 2, retained messages, and LWT. That raises the question of whether Mosquitto is still needed, collapsing the stack to a single broker.

**Decision:** Keep Mosquitto for the MQTT plane. NATS is not used as the MQTT broker.

**Rationale:**

- The project's core lesson is the *two-plane* separation: MQTT as the living control/telemetry plane, NATS JetStream as the immutable fact/history plane — the same action produces a command *and* a fact. Folding MQTT into NATS would put both planes in one server, and NATS backs QoS 1/2 subscriptions and retained messages with JetStream under the hood, making the command-vs-event distinction invisible.
- NATS's QoS 2 has a documented caveat: messages may be redelivered out of original order. TASK.md 4.6 exists to demonstrate exactly-once for critical operations; that caveat muddies precisely what the task is meant to teach.
- This is a learning project: the QoS/retain/LWT milestones are about learning MQTT proper. Against the NATS compat layer you would be learning NATS's MQTT quirks (no MQTT 5, no shared subscriptions, auto-created `$MQTT_*` streams) rather than the protocol.

**Consequences:**

- docker-compose runs two brokers (Mosquitto + NATS), one extra container to operate.
- Trade-off re-evaluated if the broker count ever becomes a real operational cost.

## Revisit triggers

- If NATS adds MQTT 5 support and/or in-order QoS 2 redelivery.
- If the two-plane separation is deliberately abandoned as a learning goal.
