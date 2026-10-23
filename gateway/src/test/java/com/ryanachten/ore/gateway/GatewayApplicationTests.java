package com.ryanachten.ore.gateway;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ryanachten.ore.common.services.SnsTopicArnResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorRequest;
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorResponse;
import software.amazon.awssdk.services.kinesis.model.ListShardsRequest;
import software.amazon.awssdk.services.kinesis.model.ListShardsResponse;
import software.amazon.awssdk.services.kinesis.model.Shard;
import software.amazon.awssdk.services.sns.SnsClient;

@SpringBootTest
class GatewayApplicationTests {

  @MockitoBean private SnsClient snsClient;

  @MockitoBean private SnsTopicArnResolver snsTopicArnResolver;

  @MockitoBean private KinesisClient kinesisClient;

  @BeforeEach
  void setUp() {
    var shard = Shard.builder().shardId("shardId-000000000000").build();
    when(kinesisClient.listShards(any(ListShardsRequest.class)))
        .thenReturn(ListShardsResponse.builder().shards(shard).build());
    when(kinesisClient.getShardIterator(any(GetShardIteratorRequest.class)))
        .thenReturn(GetShardIteratorResponse.builder().shardIterator("iterator").build());
  }

  @Test
  void contextLoads() {}
}
