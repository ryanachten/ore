package com.ryanachten.ore.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EventEnvelopeTest {

  @Test
  void versionDefaultsToOneWhenNotProvided() {
    var envelope = new EventEnvelope("evt-1", "sim.tick", 42L, "world", 0, Map.of());
    assertEquals(1, envelope.version());
  }

  @Test
  void preservesAllFields() {
    Map<String, Object> payload = Map.of("seed", 7);
    var envelope = new EventEnvelope("evt-1", "sim.tick", 42L, "world", 1, payload);

    assertEquals("evt-1", envelope.id());
    assertEquals("sim.tick", envelope.type());
    assertEquals(42L, envelope.tick());
    assertEquals("world", envelope.source());
    assertEquals(1, envelope.version());
    assertEquals(payload, envelope.payload());
  }

  @Test
  void payloadIsCopiedAtConstruction() {
    Map<String, Object> mutable = new HashMap<>(Map.of("seed", 7));
    var envelope = new EventEnvelope("evt-1", "sim.tick", 42L, "world", 1, mutable);

    mutable.put("seed", 999);

    assertEquals(7, envelope.payload().get("seed"));
  }

  @Test
  void returnedPayloadIsUnmodifiable() {
    var envelope = new EventEnvelope("evt-1", "sim.tick", 42L, "world", 1, Map.of("seed", 7));

    assertThrows(UnsupportedOperationException.class, () -> envelope.payload().put("other", 1));
  }

  @Test
  void nestedMutableValuesAreDeepCopiedAtConstruction() {
    Map<String, Object> inner = new HashMap<>(Map.of("k", "v"));
    List<Object> list = new ArrayList<>(List.of("a", inner));
    Map<String, Object> payload = new HashMap<>(Map.of("inner", inner, "list", list));

    final var envelope = new EventEnvelope("evt-1", "sim.tick", 42L, "world", 1, payload);

    inner.put("k", "mutated");
    list.set(1, "mutated");
    list.add("extra");
    payload.put("extra", "top-level");

    assertEquals(Map.of("k", "v"), envelope.payload().get("inner"));
    assertEquals(List.of("a", Map.of("k", "v")), envelope.payload().get("list"));
    assertNull(envelope.payload().get("extra"));
  }
}
