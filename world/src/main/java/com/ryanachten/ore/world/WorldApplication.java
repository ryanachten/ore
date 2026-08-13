package com.ryanachten.ore.world;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Spring Boot entry point for the world service. */
@SpringBootApplication
@EnableScheduling
public class WorldApplication {

  /** Starts the world application context. */
  public static void main(String[] args) {
    SpringApplication.run(WorldApplication.class, args);
  }
}
