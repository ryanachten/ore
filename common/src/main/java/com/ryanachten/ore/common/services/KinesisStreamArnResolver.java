package com.ryanachten.ore.common.services;

import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamSummaryRequest;

public class KinesisStreamArnResolver {
  private final KinesisClient kinesisClient;
  private final ConcurrentHashMap<String, String> arnCache = new ConcurrentHashMap<>();

  public KinesisStreamArnResolver(KinesisClient kinesisClient) {
    this.kinesisClient = kinesisClient;
  }

  public String resolve(String topicName) {
    return arnCache.computeIfAbsent(topicName, this::getArn);
  }

  private String getArn(String streamName) {
    var summary =
        kinesisClient.describeStreamSummary(
            DescribeStreamSummaryRequest.builder().streamName(streamName).build());
    return summary.streamDescriptionSummary().streamARN();
  }
}
