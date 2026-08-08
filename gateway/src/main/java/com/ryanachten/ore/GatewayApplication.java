package com.ryanachten.ore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import com.ryanachten.ore.services.NotificationService;

@ConfigurationPropertiesScan
@SpringBootApplication
public class GatewayApplication {

	public static void main(String[] args) {
		// TODO: this is just for testing, will be replaced with controller layer later
		var ctx = SpringApplication.run(GatewayApplication.class, args);
		var notificationService = ctx.getBean(NotificationService.class);
		notificationService.listTopics();
	}

}
