package com.ryanachten.ore.common.contracts;

import java.util.UUID;

public record Vehicle(UUID id, VehicleState state, Position position) {}
