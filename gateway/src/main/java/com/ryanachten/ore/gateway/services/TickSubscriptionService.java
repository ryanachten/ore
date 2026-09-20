package com.ryanachten.ore.gateway.services;

import com.ryanachten.ore.common.SnsTopics;
import com.ryanachten.ore.common.config.SnsTopicResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.ConfirmSubscriptionRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import tools.jackson.databind.ObjectMapper;

@Service
public class TickSubscriptionService {
  private static final Logger log = LoggerFactory.getLogger(TickSubscriptionService.class);
  private final SnsClient snsClient;
  private final String topicArn;

  @Value("${gateway.host.uri}")
  private String gatewayHostUri;

  @Value("${gateway.host.protocol}")
  private String protocol;

  public TickSubscriptionService(
      SnsClient snsClient, SnsTopicResolver snsTopicResolver, ObjectMapper objectMapper) {
    this.snsClient = snsClient;
    this.topicArn = snsTopicResolver.resolve(SnsTopics.ORE_SIM);
  }

  @EventListener(WebServerInitializedEvent.class)
  public void registerSubscription() {
    var request =
        SubscribeRequest.builder()
            .topicArn(topicArn)
            .endpoint(gatewayHostUri + "/tick/subscription")
            .protocol(protocol)
            .build();

    snsClient.subscribe(request);
  }

  public void confirmSubscription(String topicArn, String token, String subscriptionUrl)
      throws IllegalArgumentException {

    if (!topicArn.equals(this.topicArn)) {
      throw new IllegalArgumentException(
          "Invalid topic ARN provided in confirmation request: " + topicArn);
    }

    var request = ConfirmSubscriptionRequest.builder().topicArn(topicArn).token(token).build();

    snsClient.confirmSubscription(request);

    log.info("Confirmed tick subscription at URL {}", subscriptionUrl);
  }
}
