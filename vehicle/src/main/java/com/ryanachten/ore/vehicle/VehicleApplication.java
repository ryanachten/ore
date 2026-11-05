package com.ryanachten.ore.vehicle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.resilience.annotation.EnableResilientMethods;

@SpringBootApplication
@EnableResilientMethods
public class VehicleApplication {

  static void main(String[] args) {
    SpringApplication.run(VehicleApplication.class, args);
  }
}
