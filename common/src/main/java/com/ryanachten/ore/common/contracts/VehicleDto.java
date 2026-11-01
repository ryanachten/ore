package com.ryanachten.ore.common.contracts;

import java.util.UUID;

public record VehicleDto(UUID id, VehicleState state, PositionDto position) {}
