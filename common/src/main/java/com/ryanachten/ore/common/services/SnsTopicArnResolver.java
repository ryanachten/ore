package com.ryanachten.ore.common.services;

import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.Topic;

public class SnsTopicArnResolver {
  private final SnsClient snsClient;
  private final ConcurrentHashMap<String, String> arnCache = new ConcurrentHashMap<>();

  public SnsTopicArnResolver(SnsClient snsClient) {
    this.snsClient = snsClient;
  }

  public String resolve(String topicName) {
    return arnCache.computeIfAbsent(topicName, this::searchTopics);
  }

  private String searchTopics(String topicName) {
    return snsClient.listTopicsPaginator().stream()
        .flatMap(res -> res.topics().stream())
        .map(Topic::topicArn)
        .filter(arn -> arn.endsWith(":" + topicName))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException("Unable to find topic ARN for topic name: " + topicName));
  }
}
