package com.ryanachten.ore.vehicle.controllers;

import com.ryanachten.ore.common.models.SnsTopics;
import com.ryanachten.ore.common.services.SnsSubscriptionService;
import com.ryanachten.ore.common.services.SnsTopicResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.sns.SnsClient;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/tick")
public class TickController {
  private static final Logger log = LoggerFactory.getLogger(TickController.class);
  private final SnsSubscriptionService snsSubscriptionService;
  private final ObjectMapper objectMapper;

  public TickController(
      ObjectMapper objectMapper,
      SnsClient snsClient,
      SnsTopicResolver snsTopicResolver,
      @Value("${vehicle.host.uri}") String hostUri,
      @Value("${vehicle.host.protocol}") String protocol) {

    var topicArn = snsTopicResolver.resolve(SnsTopics.ORE_SIM);

    this.objectMapper = objectMapper;
    this.snsSubscriptionService =
        new SnsSubscriptionService(snsClient, topicArn, protocol, hostUri + "/tick/subscription");
  }

  @EventListener(WebServerInitializedEvent.class)
  public void initController() {
    snsSubscriptionService.registerSubscription();
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
          snsSubscriptionService.confirmSubscription(topicArn, token, subscriptionUrl);
        } catch (IllegalArgumentException ex) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        break;
      case "Notification":
        var message = payload.get("Message");
        log.info("received notification: {}", message.stringValue());
        break;
      case "UnsubscribeConfirmation":
        log.info("Tick unsubscribed");
        break;
      default:
        log.warn("Unexpected SNS message type: {}", messageType);
    }
  }
}
