package com.ryanachten.ore.gateway.controllers;

import com.ryanachten.ore.common.SnsTopics;
import com.ryanachten.ore.common.config.SnsTopicResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.ConfirmSubscriptionRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import tools.jackson.databind.ObjectMapper;

/** Controller that manages the SIM tick subscription lifecycle with SNS. */
@RestController
@RequestMapping("/tick")
public class TickController {

  private static final Logger log = LoggerFactory.getLogger(TickController.class);
  private final SnsClient snsClient;
  private final SnsTopicResolver snsTopicResolver;
  private final ObjectMapper objectMapper;

  @Value("${gateway.host.uri}")
  private String gatewayHostUri;

  @Value("${gateway.host.protocol}")
  private String protocol;

  /**
   * Constructs a new {@code TickController}.
   *
   * @param snsClient the SNS client used to manage subscriptions
   * @param snsTopicResolver the resolver for topic ARNs
   */
  public TickController(
      SnsClient snsClient, SnsTopicResolver snsTopicResolver, ObjectMapper objectMapper) {
    this.snsClient = snsClient;
    this.snsTopicResolver = snsTopicResolver;
    this.objectMapper = objectMapper;
  }

  /** Registers an HTTP subscription endpoint for SIM tick events with SNS. */
  @EventListener(WebServerInitializedEvent.class)
  public void registerSubscription() {
    var topicArn = snsTopicResolver.resolve(SnsTopics.ORE_SIM);

    var request =
        SubscribeRequest.builder()
            .topicArn(topicArn)
            .endpoint(gatewayHostUri + "/tick/subscription")
            .protocol(protocol)
            .build();

    snsClient.subscribe(request);
  }

  /**
   * Handles incoming SNS messages for subscription confirmation, notifications, and unsubscribe
   * confirmations.
   *
   * @param messageType the SNS message type header value
   * @param jsonPayload the SNS message payload
   */
  @PostMapping("/subscription")
  public void handleSubscription(
      @RequestHeader(value = "x-amz-sns-message-type") String messageType,
      @RequestBody String jsonPayload) {

    var payload = objectMapper.readTree(jsonPayload);
    // TODO: move logic into service layer?
    switch (messageType) {
      // Send GET request to subscription URL to confirm subscription
      case "SubscriptionConfirmation":
        var token = payload.get("Token").asString();
        var topicArn = payload.get("TopicArn").asString();
        var subscriptionUrl = payload.get("SubscribeURL").asString();

        var request = ConfirmSubscriptionRequest.builder().topicArn(topicArn).token(token).build();

        snsClient.confirmSubscription(request);

        log.info("Confirmed tick subscription at URL {}", subscriptionUrl);
        break;
      // Process notification payload
      case "Notification":
        var subject = payload.get("Subject");
        var message = payload.get("Message");
        log.info("Received tick notification with subject: {} and payload: {}", subject, payload);
        break;
      case "UnsubscribeConfirmation":
        log.info("Tick unsubscribed");
        break;
      default:
        log.warn("Unexpected SNS message type: {}", messageType);
    }
  }
}
