package com.ryanachten.ore.vehicle.services;

import com.ryanachten.ore.common.config.KinesisStreams;
import com.ryanachten.ore.common.contracts.EventEnvelope;
import com.ryanachten.ore.common.contracts.EventType;
import com.ryanachten.ore.common.contracts.Position;
import com.ryanachten.ore.common.contracts.Vehicle;
import com.ryanachten.ore.common.contracts.VehicleState;
import com.ryanachten.ore.common.services.KinesisStreamArnResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequestEntry;
import tools.jackson.databind.ObjectMapper;

@Service
public class VehicleService {
  private static final Logger log = LoggerFactory.getLogger(VehicleService.class);

  private final List<Vehicle> vehicles = new ArrayList<>();
  private final KinesisClient kinesisClient;
  private final String streamArn;
  private final ObjectMapper objectMapper;

  private long lastTick = Long.MIN_VALUE;
  private static final String SOURCE = "vehicle";

  public VehicleService(
      KinesisClient kinesisClient,
      KinesisStreamArnResolver kinesisStreamArnResolver,
      ObjectMapper objectMapper) {
    // TODO: placeholder seeding of vehicles, will eventually be provisioned on an event basis
    vehicles.add(new Vehicle(UUID.randomUUID(), VehicleState.IDLE, new Position(0, 0)));

    this.kinesisClient = kinesisClient;
    this.streamArn = kinesisStreamArnResolver.resolve(KinesisStreams.ORE_FACTS);
    this.objectMapper = objectMapper;
  }

  // TODO: this is placeholder tick handling, will eventually conduct business logic like wayfinding
  public synchronized void handleTick(long tick) {
    // Prevent duplicate or out of order events triggering actions
    if (tick <= lastTick) {
      return;
    }
    lastTick = tick;

    var requestEntries = new ArrayList<PutRecordsRequestEntry>();
    for (int i = 0; i < vehicles.size(); i++) {
      var updatedVehicle = incrementVehicle(vehicles.get(i));
      requestEntries.add(createPublishRequest(updatedVehicle, tick));
      vehicles.set(i, updatedVehicle);
    }

    var request = PutRecordsRequest.builder().streamARN(streamArn).records(requestEntries).build();
    var result = kinesisClient.putRecords(request);

    if (result.failedRecordCount() > 0) {
      log.error(
          "Vehicle update failed to publish on Kinesis. {} records failed out of {}",
          result.failedRecordCount(),
          requestEntries.size());
    } else {
      log.info(
          "Vehicle update publish on Kinesis successful. {} records published",
          requestEntries.size());
    }
  }

  private Vehicle incrementVehicle(Vehicle vehicle) {
    return new Vehicle(
        vehicle.id(),
        VehicleState.TRAVELING,
        new Position(vehicle.position().x(), vehicle.position().y() + 1));
  }

  private PutRecordsRequestEntry createPublishRequest(Vehicle vehicle, long tick) {
    var envelope =
        new EventEnvelope(
            UUID.randomUUID(), EventType.VEHICLE_TICK, tick, SOURCE, 1, Map.of("vehicle", vehicle));

    var envelopePayload = objectMapper.writeValueAsBytes(envelope);
    return PutRecordsRequestEntry.builder()
        .partitionKey("vehicle-" + vehicle.id().toString())
        .data(SdkBytes.fromByteArray(envelopePayload))
        .build();
  }
}
