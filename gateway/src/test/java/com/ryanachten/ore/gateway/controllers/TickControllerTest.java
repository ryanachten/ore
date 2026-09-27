package com.ryanachten.ore.gateway.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ryanachten.ore.common.EventEnvelope;
import com.ryanachten.ore.common.EventType;
import com.ryanachten.ore.common.SnsTopics;
import com.ryanachten.ore.common.config.SnsTopicResolver;
import com.ryanachten.ore.gateway.services.SocketConnectionHandler;
import com.ryanachten.ore.gateway.services.TickSubscriptionService;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.ConfirmSubscriptionRequest;
import tools.jackson.databind.ObjectMapper;

class TickControllerTest {

  private static final String EXPECTED_ARN = "arn:aws:sns:us-east-1:000000000000:ore-sim";
  private static final String TOKEN = "abc123";
  private static final String SUBSCRIBE_URL =
      "https://sns.us-east-1.amazonaws.com/?Action=ConfirmSubscription&Token=" + TOKEN;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private SnsClient snsClient;
  private SocketConnectionHandler socketConnectionHandler;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    snsClient = mock(SnsClient.class);
    socketConnectionHandler = mock(SocketConnectionHandler.class);

    var snsTopicResolver = mock(SnsTopicResolver.class);
    when(snsTopicResolver.resolve(SnsTopics.ORE_SIM)).thenReturn(EXPECTED_ARN);

    var tickSubscriptionService =
        new TickSubscriptionService(snsClient, snsTopicResolver, objectMapper);

    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new TickController(objectMapper, tickSubscriptionService, socketConnectionHandler))
            .build();
  }

  @Test
  void subscriptionConfirmationDispatchesToConfirmSubscription() throws Exception {
    String body =
        objectMapper
            .createObjectNode()
            .put("Type", "SubscriptionConfirmation")
            .put("TopicArn", EXPECTED_ARN)
            .put("Token", TOKEN)
            .put("SubscribeURL", SUBSCRIBE_URL)
            .toString();

    mockMvc
        .perform(
            post("/tick/subscription")
                .header("x-amz-sns-message-type", "SubscriptionConfirmation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk());

    var requestCaptor = ArgumentCaptor.forClass(ConfirmSubscriptionRequest.class);
    verify(snsClient).confirmSubscription(requestCaptor.capture());
    assertThat(requestCaptor.getValue().topicArn()).isEqualTo(EXPECTED_ARN);
    assertThat(requestCaptor.getValue().token()).isEqualTo(TOKEN);
  }

  @Test
  void confirmationForUnexpectedTopicIsRejected() throws Exception {
    String body =
        objectMapper
            .createObjectNode()
            .put("Type", "SubscriptionConfirmation")
            .put("TopicArn", "arn:aws:sns:us-east-1:000000000000:other-topic")
            .put("Token", TOKEN)
            .put("SubscribeURL", SUBSCRIBE_URL)
            .toString();

    mockMvc
        .perform(
            post("/tick/subscription")
                .header("x-amz-sns-message-type", "SubscriptionConfirmation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());

    verify(snsClient, never()).confirmSubscription(any(ConfirmSubscriptionRequest.class));
  }

  @Test
  void notificationForwardsTheSnsMessageBodyUnchanged() throws Exception {
    String body = notificationBody(envelopeJson(42L));

    mockMvc
        .perform(
            post("/tick/subscription")
                .header("x-amz-sns-message-type", "Notification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk());

    verify(socketConnectionHandler).broadcastEvent(envelopeJson(42L));
  }

  @Test
  void notificationIsBroadcastWithoutValidatingItIsAnEnvelope() throws Exception {
    // The gateway is deliberately a transparent pipe: it does not parse the SNS
    // message body, so anything delivered on the topic reaches clients verbatim.
    // This pins that decision - it should change deliberately, not by accident.
    String body = notificationBody("not-an-envelope");

    mockMvc
        .perform(
            post("/tick/subscription")
                .header("x-amz-sns-message-type", "Notification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk());

    verify(socketConnectionHandler).broadcastEvent("not-an-envelope");
  }

  @Test
  void unsubscribeConfirmationIsAcknowledged() throws Exception {
    String body =
        objectMapper
            .createObjectNode()
            .put("Type", "UnsubscribeConfirmation")
            .put("TopicArn", EXPECTED_ARN)
            .toString();

    mockMvc
        .perform(
            post("/tick/subscription")
                .header("x-amz-sns-message-type", "UnsubscribeConfirmation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk());

    verify(socketConnectionHandler, never()).broadcastEvent(any());
  }

  private String envelopeJson(long tick) throws Exception {
    return objectMapper.writeValueAsString(tickEnvelope(tick));
  }

  private EventEnvelope tickEnvelope(long tick) {
    return new EventEnvelope(
        UUID.fromString("e7c5f4c0-0000-4000-8000-000000000001"),
        EventType.SIM_TICK,
        tick,
        "world",
        1,
        Map.of("seed", 7));
  }

  private String notificationBody(String snsMessage) {
    return objectMapper
        .createObjectNode()
        .put("Type", "Notification")
        .put("TopicArn", EXPECTED_ARN)
        .put("Message", snsMessage)
        .toString();
  }
}
