package com.ryanachten.ore.vehicle.models;

import java.util.UUID;

public record Vehicle(UUID id, VehicleState state, Position position) {}
