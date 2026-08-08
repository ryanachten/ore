package com.ryanachten.ore.services;

import com.ryanachten.ore.config.AwsConfiguration;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SnsException;

/** Publishes and queries notifications through AWS SNS. */
@Service
public class NotificationService {
  private final SnsClient snsClient;
  private static final Logger logger = Logger.getLogger(NotificationService.class.getName());

  /** Builds an SNS client from the supplied AWS configuration. */
  public NotificationService(AwsConfiguration config) {

    var credentialsProvider =
        StaticCredentialsProvider.create(
            AwsBasicCredentials.create(config.getAccessKeyId(), config.getSecretAccessKeyId()));

    this.snsClient =
        SnsClient.builder()
            .credentialsProvider(credentialsProvider)
            .endpointOverride(config.getEndpointOverride())
            .build();
  }

  /** Logs the ARNs of all topics available to the configured SNS account. */
  public void listTopics() {
    try {
      var listTopics = snsClient.listTopicsPaginator();
      listTopics.stream()
          .flatMap(r -> r.topics().stream())
          .forEach(content -> logger.info(" Topic ARN: " + content.topicArn()));

    } catch (SnsException e) {
      throw new IllegalStateException(
          "Failed to list SNS topics: " + e.awsErrorDetails().errorMessage(), e);
    }
  }
}
