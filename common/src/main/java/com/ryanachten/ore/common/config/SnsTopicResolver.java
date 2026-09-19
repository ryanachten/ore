package com.ryanachten.ore.common.config;

import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.Topic;

public class SnsTopicResolver {
  private final SnsClient snsClient;
  private final ConcurrentHashMap<String, String> arnCache = new ConcurrentHashMap<>();

  public SnsTopicResolver(SnsClient snsClient) {
    this.snsClient = snsClient;
  }

  public String resolve(String topicName) {
    return arnCache.computeIfAbsent(topicName, this::searchTopics);
  }

  public String searchTopics(String topicName) {
    return snsClient.listTopicsPaginator().stream()
        .flatMap(res -> res.topics().stream())
        .map(Topic::topicArn)
        .filter(arn -> arn.contains(topicName))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException("Unable to find topic ARN for topic name: " + topicName));
  }
}
