package com.ryanachten.ore.vehicle.models;

import com.ryanachten.ore.common.contracts.VehicleState;
import java.util.UUID;

public record Vehicle(UUID id, VehicleState state, Position position) {}
