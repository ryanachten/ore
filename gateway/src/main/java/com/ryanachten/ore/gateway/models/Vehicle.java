package com.ryanachten.ore.gateway.models;

import com.ryanachten.ore.common.contracts.VehicleState;
import java.util.UUID;

public record Vehicle(UUID id, VehicleState state, Position position) {}
