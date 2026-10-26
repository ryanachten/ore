package com.ryanachten.ore.gateway.repositories;

import com.ryanachten.ore.common.contracts.Vehicle;
import com.ryanachten.ore.gateway.models.CurrentWorldState;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class InmemoryWorldStateRepository implements WorldStateRepository {

  private final AtomicLong maxTick = new AtomicLong(0);
  private final ConcurrentHashMap<UUID, Vehicle> vehicles = new ConcurrentHashMap<>();

  @Override
  public void updateState(Vehicle vehicle, long tick) {
    maxTick.accumulateAndGet(tick, Math::max);
    vehicles.put(vehicle.id(), vehicle);
  }

  @Override
  public CurrentWorldState getState() {
    return new CurrentWorldState(maxTick.get(), vehicles.values());
  }
}
