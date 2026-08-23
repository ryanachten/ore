package com.ryanachten.ore.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Spring Boot entry point for the gateway service. */
@SpringBootApplication
public class GatewayApplication {

  /** Starts the gateway application context. */
  public static void main(String[] args) {
    SpringApplication.run(GatewayApplication.class, args);
  }
}
