package com.ryanachten.ore.world.services;

import com.ryanachten.ore.common.EventEnvelope;
import com.ryanachten.ore.common.EventType;
import com.ryanachten.ore.common.config.SnsTopicResolver;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SnsException;
import tools.jackson.databind.ObjectMapper;

/** Publishes the global {@code sim.tick} clock event on a fixed schedule. */
@Service
public class TickScheduler {
  private static final String TOPIC_NAME = "ore-sim";
  private static final String SOURCE = "world";
  private static final String EVENT_TYPE = EventType.SIM_TICK;

  private final SnsClient snsClient;
  private final SnsTopicResolver snsTopicResolver;
  private final ObjectMapper objectMapper;

  private static final Logger logger = Logger.getLogger(TickScheduler.class.getName());
  private final AtomicLong tickCount = new AtomicLong(1);

  /** Creates the tick publisher with the SNS client, topic resolver and JSON mapper. */
  public TickScheduler(
      SnsClient snsClient, SnsTopicResolver snsTopicResolver, ObjectMapper objectMapper) {
    this.snsClient = snsClient;
    this.snsTopicResolver = snsTopicResolver;
    this.objectMapper = objectMapper;
  }

  /** Publishes the next tick as an envelope with an incrementing tick number. */
  @Scheduled(fixedDelayString = "${tick.rate}")
  public void tick() {
    var version = 1;
    var event =
        new EventEnvelope(
            UUID.randomUUID(), EVENT_TYPE, tickCount.longValue(), SOURCE, version, Map.of());

    var jsonPayload = objectMapper.writeValueAsString(event);

    var msgAttributes = new HashMap<String, MessageAttributeValue>();
    msgAttributes.put(
        "eventType",
        MessageAttributeValue.builder().dataType("String").stringValue(EVENT_TYPE).build());

    var request =
        PublishRequest.builder()
            .messageAttributes(msgAttributes)
            .message(jsonPayload)
            .topicArn(snsTopicResolver.resolve(TOPIC_NAME))
            .build();

    try {
      snsClient.publish(request);
    } catch (SnsException e) {
      throw new IllegalStateException(
          "Failed to publish tick: " + e.awsErrorDetails().errorMessage(), e);
    }

    logger.info("Published tick: " + tickCount);

    tickCount.getAndIncrement();
  }
}
