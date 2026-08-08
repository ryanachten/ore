package com.ryanachten.ore;

import com.ryanachten.ore.services.NotificationService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Spring Boot entry point for the gateway service. */
@ConfigurationPropertiesScan
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
