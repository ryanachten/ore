package com.ryanachten.ore.gateway.controllers;

import com.ryanachten.ore.common.EventEnvelope;
import com.ryanachten.ore.gateway.services.TickSubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/tick")
public class TickController {

  private static final Logger log = LoggerFactory.getLogger(TickController.class);
  private final TickSubscriptionService tickSubscriptionService;
  private final ObjectMapper objectMapper;

  public TickController(
      ObjectMapper objectMapper, TickSubscriptionService tickSubscriptionService) {
    this.tickSubscriptionService = tickSubscriptionService;
    this.objectMapper = objectMapper;
  }

  @PostMapping("/subscription")
  public void handleSubscription(
      @RequestHeader(value = "x-amz-sns-message-type") String messageType,
      @RequestBody String jsonPayload) {

    var payload = objectMapper.readTree(jsonPayload);

    switch (messageType) {
      case "SubscriptionConfirmation":
        var token = payload.get("Token").asString();
        var topicArn = payload.get("TopicArn").asString();
        var subscriptionUrl = payload.get("SubscribeURL").asString();
        try {
          tickSubscriptionService.confirmSubscription(topicArn, token, subscriptionUrl);
        } catch (IllegalArgumentException ex) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        break;
      case "Notification":
        var message = payload.get("Message");
        var event = objectMapper.readValue(message.stringValue(), EventEnvelope.class);
        log.info("Received tick event: {}", event.tick());
        break;
      case "UnsubscribeConfirmation":
        log.info("Tick unsubscribed");
        break;
      default:
        log.warn("Unexpected SNS message type: {}", messageType);
    }
  }
}
