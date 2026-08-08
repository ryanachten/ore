package com.ryanachten.ore;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * An immutable event envelope exchanged on the event bus.
 *
 * <p>Records the event identity, origin and a free-form payload. The payload is defensively copied
 * into an unmodifiable map on construction and on every read so no caller can mutate event data.
 * Nested Maps and Lists are recursively copied and frozen as well; other mutable values are stored
 * by reference.
 *
 * @param id a unique identifier for the event
 * @param type the logical event type name
 * @param tick the monotonically increasing tick at which the event occurred
 * @param source the identifier of the component that emitted the event
 * @param version the payload schema version; defaults to 1 when omitted
 * @param payload the event data, stored as an unmodifiable map
 */
public record EventEnvelope(
    String id, String type, long tick, String source, int version, Map<String, Object> payload) {
  /** Applies defaults and takes a defensive immutable copy of the payload. */
  public EventEnvelope {
    if (version == 0) {
      version = 1;
    }
    payload = freeze(payload);
  }

  @Override
  public Map<String, Object> payload() {
    return freeze(payload);
  }

  private static Map<String, Object> freeze(Map<String, Object> source) {
    return source.entrySet().stream()
        .collect(
            Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> freezeValue(entry.getValue())));
  }

  private static Object freezeValue(Object value) {
    if (value instanceof Map<?, ?> map) {
      return map.entrySet().stream()
          .collect(
              Collectors.toUnmodifiableMap(
                  Map.Entry::getKey, entry -> freezeValue(entry.getValue())));
    }
    if (value instanceof List<?> list) {
      return list.stream().map(EventEnvelope::freezeValue).toList();
    }
    return value;
  }
}
