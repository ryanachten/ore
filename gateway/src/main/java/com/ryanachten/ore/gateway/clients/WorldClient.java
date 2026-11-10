package com.ryanachten.ore.gateway.clients;

import com.ryanachten.ore.common.contracts.WorldDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class WorldClient {
  private final RestClient client;

  public WorldClient(@Value("${world.host.uri}") String worldHostUri) {
    this.client = RestClient.builder().baseUrl(worldHostUri).build();
  }

  @Retryable
  public WorldDto getWorldDefinition() {
    var result = client.get().uri("/world").retrieve().toEntity(WorldDto.class);
    return result.getBody();
  }
}
