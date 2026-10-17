package com.ryanachten.ore.gateway.repositories;

import com.ryanachten.ore.common.contracts.Vehicle;
import com.ryanachten.ore.gateway.models.CurrentWorldState;

public interface WorldStateRepository {
  void updateState(Vehicle vehicle, long tick);

  CurrentWorldState getState();
}
