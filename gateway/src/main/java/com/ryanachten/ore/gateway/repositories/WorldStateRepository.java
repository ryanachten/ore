package com.ryanachten.ore.gateway.repositories;

import com.ryanachten.ore.gateway.models.CurrentWorldState;
import com.ryanachten.ore.gateway.models.Vehicle;

public interface WorldStateRepository {
  void updateState(Vehicle vehicle, long tick);

  CurrentWorldState getState();
}
