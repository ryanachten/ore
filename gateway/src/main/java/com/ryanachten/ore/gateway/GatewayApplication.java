package com.ryanachten.ore.gateway;

import com.ryanachten.ore.gateway.services.NotificationService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Spring Boot entry point for the gateway service. */
@SpringBootApplication
public class GatewayApplication {

  /**
   * Starts the gateway application context and exercises the notification service.
   *
   * <p>TODO: replace the smoke-test call with a controller layer that reacts to incoming events.
   */
  public static void main(String[] args) {
    var ctx = SpringApplication.run(GatewayApplication.class, args);
    var notificationService = ctx.getBean(NotificationService.class);
    notificationService.listTopics();
  }
}
