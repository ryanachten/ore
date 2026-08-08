package com.ryanachten.ore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
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
}
