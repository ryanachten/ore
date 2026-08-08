package com.ryanachten.ore;

import java.util.Map;

public record EventEnvelope(
		String id,
		String type,
		long tick,
		String source,
		int version,
		Map<String, Object> payload) {
	public EventEnvelope {
		if (version == 0)
			version = 1;
	}
}