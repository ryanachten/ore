package com.ryanachten.ore.world.controllers;

import com.ryanachten.ore.common.contracts.BoundsDto;
import com.ryanachten.ore.common.contracts.PositionDto;
import com.ryanachten.ore.common.contracts.WorldDto;
import com.ryanachten.ore.world.config.BoundsProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/world")
public class WorldController {

  private final BoundsProperties bounds;

  public WorldController(BoundsProperties bounds) {
    this.bounds = bounds;
  }

  @GetMapping()
  public WorldDto getWorldDefinition() {
    var baseOrigin = new PositionDto(bounds.width() / 2, bounds.height() / 2);
    return new WorldDto(new BoundsDto(bounds.width(), bounds.height()), baseOrigin);
  }
}
