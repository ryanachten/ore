package com.ryanachten.ore.gateway.controllers;

import com.ryanachten.ore.common.contracts.WorldDto;
import com.ryanachten.ore.gateway.clients.WorldClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/world")
public class WorldController {
  private final WorldClient worldClient;

  public WorldController(WorldClient worldClient) {
    this.worldClient = worldClient;
  }

  @GetMapping()
  public WorldDto getWorldDefinition() {
    return worldClient.getWorldDefinition();
  }
}
