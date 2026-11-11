package com.ryanachten.ore.gateway.services;

import com.ryanachten.ore.common.config.KinesisStreams;
import com.ryanachten.ore.common.contracts.EventEnvelopeDto;
import com.ryanachten.ore.common.contracts.VehicleDto;
import com.ryanachten.ore.gateway.config.ModelMapper;
import com.ryanachten.ore.gateway.repositories.WorldStateRepository;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.GetRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.GetRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorRequest;
import software.amazon.awssdk.services.kinesis.model.ListShardsRequest;
import software.amazon.awssdk.services.kinesis.model.Record;
import software.amazon.awssdk.services.kinesis.model.Shard;
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType;
import tools.jackson.databind.ObjectMapper;

@Service
public class VehicleUpdateConsumer implements SmartLifecycle {
  private static final Logger log = LoggerFactory.getLogger(VehicleUpdateConsumer.class);
  private final KinesisClient kinesisClient;
  private final ObjectMapper objectMapper;
  private final HashMap<String, String> shardIterators = new HashMap<>();
  private final SocketConnectionHandler socketConnectionHandler;
  private final WorldStateRepository vehicleRepository;
  private final ModelMapper mapper;
  private boolean isRunning = false;
  private ScheduledExecutorService scheduler;

  private static final int POLL_PERIOD_MS = 1000;

  public VehicleUpdateConsumer(
      KinesisClient kinesisClient,
      ObjectMapper objectMapper,
      SocketConnectionHandler socketConnectionHandler,
      WorldStateRepository vehicleRepository,
      ModelMapper mapper) {
    this.kinesisClient = kinesisClient;
    this.objectMapper = objectMapper;
    this.socketConnectionHandler = socketConnectionHandler;
    this.vehicleRepository = vehicleRepository;
    this.mapper = mapper;
  }

  @Override
  public void start() {
    try {
      var shards = getShards();
      this.scheduler = Executors.newScheduledThreadPool(shards.size());
      for (var shard : shards) {
        this.shardIterators.put(shard.shardId(), getShardIterator(shard));
        scheduler.scheduleAtFixedRate(
            () -> getShardRecords(shard.shardId()), 0, POLL_PERIOD_MS, TimeUnit.MILLISECONDS);
      }
      isRunning = true;
    } catch (IllegalAccessException exception) {
      log.error("Error getting records", exception);
    }
  }

  @Override
  public void stop() {
    if (scheduler != null) {
      scheduler.shutdown();
    }
    isRunning = false;
  }

  @Override
  public boolean isRunning() {
    return isRunning;
  }

  private List<Shard> getShards() throws IllegalAccessException {
    var request = ListShardsRequest.builder().streamName(KinesisStreams.ORE_FACTS).build();
    var result = kinesisClient.listShards(request);

    if (result == null || !result.hasShards()) {
      throw new IllegalAccessException("No shards for stream can be found");
    }

    return result.shards();
  }

  /// Produces a shard iterator using TRIM_HORIZON - reads from the oldest record still retained
  /// TRIM_HORIZON is used because we don't want to miss records.
  /// The trade-off is that this can result in a burst of older records, so handling needs to be
  // idempotent
  private String getShardIterator(Shard shard) {
    var request =
        GetShardIteratorRequest.builder()
            .streamName(KinesisStreams.ORE_FACTS)
            .shardId(shard.shardId())
            .shardIteratorType(ShardIteratorType.TRIM_HORIZON)
            .build();

    var result = kinesisClient.getShardIterator(request);
    return result.shardIterator();
  }

  private void getShardRecords(String shardId) {
    try {
      var iterator = shardIterators.get(shardId);
      var response = getRecords(iterator);
      response.records().forEach(this::processRecord);

      var nextIterator = response.nextShardIterator();
      if (iterator == null) {
        log.warn("Shard {} returned no iterator. Shard has been closed", shardId);
      }
      shardIterators.put(shardId, nextIterator);
    } catch (RuntimeException exception) {
      log.error("error getting records for shard {}", shardId, exception);
    }
  }

  private GetRecordsResponse getRecords(String shardIterator) {
    var request = GetRecordsRequest.builder().shardIterator(shardIterator).limit(25).build();

    return kinesisClient.getRecords(request);
  }

  private void processRecord(Record record) {
    try {
      var data = record.data().asUtf8String();
      socketConnectionHandler.broadcastEvent(data);
      var envelope = objectMapper.readValue(data, EventEnvelopeDto.class);
      var rawVehicle = envelope.payload().get("vehicle");
      if (rawVehicle == null) {
        return;
      }
      var vehicleDto = objectMapper.convertValue(rawVehicle, VehicleDto.class);
      var vehicle = mapper.toVehicle(vehicleDto);
      vehicleRepository.updateState(vehicle, envelope.tick());
    } catch (RuntimeException exception) {
      log.error("error processing record", exception);
    }
  }
}
