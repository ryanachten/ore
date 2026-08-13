package com.ryanachten.ore.common.config;

import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.Topic;

/** Resolves SNS topic ARNs from topic names, caching resolved lookups. */
public class SnsTopicResolver {
  private final SnsClient snsClient;
  private final ConcurrentHashMap<String, String> arnCache = new ConcurrentHashMap<>();

  /** Creates a resolver backed by the given SNS client. */
  public SnsTopicResolver(SnsClient snsClient) {
    this.snsClient = snsClient;
  }

  /** Returns the ARN for the named topic, resolving and caching it on first use. */
  public String resolve(String topicName) {
    return arnCache.computeIfAbsent(topicName, this::searchTopics);
  }

  /** Searches the account's topics for the first ARN containing the given name. */
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
