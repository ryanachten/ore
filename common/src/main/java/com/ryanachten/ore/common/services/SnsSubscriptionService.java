package com.ryanachten.ore.common.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.ConfirmSubscriptionRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;

public class SnsSubscriptionService {
  private static final Logger log = LoggerFactory.getLogger(SnsSubscriptionService.class);
  private final SnsClient snsClient;
  private final String topicArn;
  private final String protocol;
  private final String endpoint;

  public SnsSubscriptionService(
      SnsClient snsClient, String topicArn, String protocol, String endpoint) {
    this.snsClient = snsClient;
    this.topicArn = topicArn;
    this.protocol = protocol;
    this.endpoint = endpoint;
  }

  public void registerSubscription() {
    var request =
        SubscribeRequest.builder().topicArn(topicArn).endpoint(endpoint).protocol(protocol).build();

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

    log.info("Confirmed topic ARN {} subscription at URL {}", topicArn, subscriptionUrl);
  }
}
