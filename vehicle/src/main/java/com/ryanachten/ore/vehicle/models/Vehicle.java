package com.ryanachten.ore.vehicle.models;

import java.util.UUID;

public final class Vehicle {
  public UUID id;
  public VehicleState state;
  public Position position;

  public Vehicle(Position position) {
    this.id = UUID.randomUUID();
    this.state = VehicleState.IDLE;
    this.position = position;
  }
}
