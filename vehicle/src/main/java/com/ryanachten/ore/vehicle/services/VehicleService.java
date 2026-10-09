package com.ryanachten.ore.vehicle.services;

import com.ryanachten.ore.common.config.KinesisStreams;
import com.ryanachten.ore.common.services.KinesisStreamArnResolver;
import com.ryanachten.ore.common.services.SnsSubscriptionService;
import com.ryanachten.ore.vehicle.models.Position;
import com.ryanachten.ore.vehicle.models.Vehicle;
import com.ryanachten.ore.vehicle.models.VehicleState;
import java.util.ArrayList;
import java.util.List;
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
  private static final Logger log = LoggerFactory.getLogger(SnsSubscriptionService.class);

  private final List<Vehicle> vehicles;
  private final KinesisClient kinesisClient;
  private final String streamArn;
  private final ObjectMapper objectMapper;

  public VehicleService(
      KinesisClient kinesisClient,
      KinesisStreamArnResolver kinesisStreamArnResolver,
      ObjectMapper objectMapper) {
    // TODO: this is placeholder seeding of vehicles, should be provisioned dynamically - but from
    // where?
    vehicles = List.of(new Vehicle(new Position(0, 0)));

    this.kinesisClient = kinesisClient;
    this.streamArn = kinesisStreamArnResolver.resolve(KinesisStreams.ORE_FACTS);
    this.objectMapper = objectMapper;
  }

  // TODO: this is placeholder tick handling, will eventually conduct business logic like wayfinding
  // etc
  public void handleTick() {
    var requestEntries = new ArrayList<PutRecordsRequestEntry>();
    for (Vehicle vehicle : vehicles) {
      vehicle.state = VehicleState.TRAVELING;
      vehicle.position = new Position(vehicle.position.x(), vehicle.position.y() + 1);
      requestEntries.add(createPublishRequest(vehicle));
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

  private PutRecordsRequestEntry createPublishRequest(Vehicle vehicle) {
    var payload = objectMapper.writeValueAsBytes(vehicle);
    return PutRecordsRequestEntry.builder()
        .partitionKey(vehicle.id.toString())
        .data(SdkBytes.fromByteArray(payload))
        .build();
  }
}
