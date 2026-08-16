package com.ryanachten.ore.gateway.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SnsException;

/** Publishes and queries notifications through AWS SNS. */
@Service
public class NotificationService {
  private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
  private final SnsClient snsClient;

  /** Service for dispatching notifications. */
  public NotificationService(SnsClient snsClient) {
    this.snsClient = snsClient;
  }

  /** Logs the ARNs of all topics available to the configured SNS account. */
  public void listTopics() {
    try {
      var listTopics = snsClient.listTopicsPaginator();
      listTopics.stream()
          .flatMap(r -> r.topics().stream())
          .forEach(content -> logger.info("Topic ARN {}", content.topicArn()));

    } catch (SnsException e) {
      throw new IllegalStateException(
          "Failed to list SNS topics: " + e.awsErrorDetails().errorMessage(), e);
    }
  }
}
