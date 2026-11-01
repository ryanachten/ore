package com.ryanachten.ore.world;

import com.ryanachten.ore.world.config.BoundsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(BoundsProperties.class)
public class WorldApplication {

  static void main(String[] args) {
    SpringApplication.run(WorldApplication.class, args);
  }
}
