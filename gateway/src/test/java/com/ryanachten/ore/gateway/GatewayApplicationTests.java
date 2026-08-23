package com.ryanachten.ore.gateway;

import com.ryanachten.ore.common.config.SnsTopicResolver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.sns.SnsClient;

@SpringBootTest
class GatewayApplicationTests {

  @MockitoBean private SnsClient snsClient;

  @MockitoBean private SnsTopicResolver snsTopicResolver;

  @Test
  void contextLoads() {}
}
