package com.ryanachten.ore.vehicle.clients;

import com.ryanachten.ore.common.contracts.WorldDto;
import com.ryanachten.ore.vehicle.config.ModelMapper;
import com.ryanachten.ore.vehicle.models.World;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class WorldClient {
  private final RestClient client;
  private final ModelMapper modelMapper;

  public WorldClient(ModelMapper modelMapper, @Value("${world.host.uri}") String worldHostUri) {
    this.client = RestClient.builder().baseUrl(worldHostUri).build();
    this.modelMapper = modelMapper;
  }

  @Retryable
  public World getWorldDefinition() {
    var result = client.get().uri("/world").retrieve().toEntity(WorldDto.class);
    var dto = result.getBody();
    return modelMapper.toWorld(dto);
  }
}
