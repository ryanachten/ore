package com.ryanachten.ore.vehicle.services;

import com.ryanachten.ore.vehicle.clients.WorldClient;
import com.ryanachten.ore.vehicle.models.World;
import org.springframework.stereotype.Service;

@Service
public class WorldDefinitionProvider {
  private final WorldClient worldClient;

  private volatile World definition;

  public WorldDefinitionProvider(WorldClient worldClient) {
    this.worldClient = worldClient;
  }

  public World getDefinition() {
    return definition;
  }

  public void load() {
    definition = worldClient.getWorldDefinition();
  }
}
