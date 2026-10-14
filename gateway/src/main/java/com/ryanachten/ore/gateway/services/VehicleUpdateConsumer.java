package com.ryanachten.ore.gateway.services;

import com.ryanachten.ore.common.config.KinesisStreams;
import com.ryanachten.ore.common.contracts.Vehicle;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.GetRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.GetRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorRequest;
import software.amazon.awssdk.services.kinesis.model.ListShardsRequest;
import software.amazon.awssdk.services.kinesis.model.Shard;
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType;
import tools.jackson.databind.ObjectMapper;

@Service
public class VehicleUpdateConsumer {
  private static final Logger log = LoggerFactory.getLogger(VehicleUpdateConsumer.class);
  private final KinesisClient kinesisClient;
  private final ObjectMapper objectMapper;
  private List<Shard> shards;
  private final HashMap<String, String> shardIterators = new HashMap<>();

  public VehicleUpdateConsumer(KinesisClient kinesisClient, ObjectMapper objectMapper) {
    this.kinesisClient = kinesisClient;
    this.objectMapper = objectMapper;
    init();
  }

  // TODO: Idiomatic shape: constructor wires dependencies only;
  // start the loop from @PostConstruct or, better, a SmartLifecycle/ApplicationRunner
  // so it starts after the context is ready and stops cleanly on shutdown.
  // Use a scheduled executor / dedicated thread(s) rather than a self-calling method.
  // Keep the poll logic separate from the lifecycle so you can unit-test it without Spring.
  private void init() {
    try {
      // TODO: refactor this as one reader per thread per shard
      // I think we'll migrate to KCL in the future
      this.shards = getShards();
      for (var shard : shards) {
        this.shardIterators.put(shard.shardId(), getShardIterator(shard));
      }
      pollForRecords();
    } catch (IllegalAccessException exception) {
      log.error("Error getting records", exception);
    }
  }

  private void pollForRecords() {
    try {
      // TODO: we'll need smoothing on the client to handle how bursty this is
      Thread.sleep(200);

      for (var shard : shards) {
        var iterator = shardIterators.get(shard.shardId());
        var response = getRecords(iterator);
        response
            .records()
            .forEach(
                r -> {
                  var data = r.data().asUtf8String();
                  // TODO: I don't understand how this is working - it should be the envelope we're
                  // parsing
                  var vehicle = objectMapper.readValue(data, Vehicle.class);
                  log.info(
                      "Retrieved vehicle update: id: {}, state: {}, x: {}, y: {}",
                      vehicle.id(),
                      vehicle.state(),
                      vehicle.position().x(),
                      vehicle.position().y());
                });

        shardIterators.put(shard.shardId(), response.nextShardIterator());
      }

      pollForRecords();
    } catch (InterruptedException exception) {
      throw new RuntimeException(exception);
    }
  }

  private List<Shard> getShards() throws IllegalAccessException {
    var request = ListShardsRequest.builder().streamName(KinesisStreams.ORE_FACTS).build();
    var result = kinesisClient.listShards(request);

    if (!result.hasShards()) {
      throw new IllegalAccessException("No shards for stream can be found");
    }

    return result.shards();
  }

  private String getShardIterator(Shard shard) {
    var request =
        GetShardIteratorRequest.builder()
            .streamName(KinesisStreams.ORE_FACTS)
            .shardId(shard.shardId())
            // TODO: apparently this should be using TRIM_HORIZON
            .shardIteratorType(ShardIteratorType.LATEST)
            .build();

    var result = kinesisClient.getShardIterator(request);
    return result.shardIterator();
  }

  private GetRecordsResponse getRecords(String shardIterator) {
    var request = GetRecordsRequest.builder().shardIterator(shardIterator).limit(25).build();

    return kinesisClient.getRecords(request);
  }
}
