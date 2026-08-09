package com.ryanachten.ore.world;

import com.ryanachten.ore.world.services.NotificationService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Spring Boot entry point for the world service. */
@SpringBootApplication
public class WorldApplication {

  /**
   * Starts the world application context.
   *
   * <p>TODO: replace the smoke-test call with a controller layer that reacts to incoming events.
   */
  public static void main(String[] args) {
    var ctx = SpringApplication.run(WorldApplication.class, args);
    var notificationService = ctx.getBean(NotificationService.class);
    notificationService.listTopics();
  }
}
