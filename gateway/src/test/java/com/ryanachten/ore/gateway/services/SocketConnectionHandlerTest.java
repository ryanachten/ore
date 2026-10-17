package com.ryanachten.ore.gateway.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ryanachten.ore.gateway.models.CurrentWorldState;
import com.ryanachten.ore.gateway.repositories.WorldStateRepository;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

class SocketConnectionHandlerTest {

  private static final String PAYLOAD = "{\"type\":\"sim.tick\",\"tick\":42}";

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final WorldStateRepository worldStateRepository = mock(WorldStateRepository.class);
  private final SocketConnectionHandler handler =
      new SocketConnectionHandler(worldStateRepository, objectMapper);

  @BeforeEach
  void setUp() {
    when(worldStateRepository.getState()).thenReturn(new CurrentWorldState(0, List.of()));
  }

  @Test
  void broadcastEventDeliversThePayloadToEveryConnectedSession() throws Exception {
    var first = openSession("first");
    var second = openSession("second");

    handler.afterConnectionEstablished(first);
    handler.afterConnectionEstablished(second);
    clearInvocations(first, second);

    handler.broadcastEvent(PAYLOAD);

    assertThat(sentPayload(first)).isEqualTo(PAYLOAD);
    assertThat(sentPayload(second)).isEqualTo(PAYLOAD);
  }

  @Test
  void broadcastStillReachesHealthySessionsWhenOneFails() throws Exception {
    var failing = openSession("failing");
    var healthy = openSession("healthy");

    handler.afterConnectionEstablished(failing);
    handler.afterConnectionEstablished(healthy);
    clearInvocations(failing, healthy);
    doThrow(new IOException("broken pipe")).when(failing).sendMessage(any());

    handler.broadcastEvent(PAYLOAD);

    assertThat(sentPayload(healthy)).isEqualTo(PAYLOAD);
  }

  @Test
  void disconnectedSessionIsNoLongerWrittenTo() throws Exception {
    var session = openSession("gone");

    handler.afterConnectionEstablished(session);
    clearInvocations(session);
    handler.afterConnectionClosed(session, CloseStatus.NORMAL);

    handler.broadcastEvent(PAYLOAD);

    verify(session, never()).sendMessage(any());
  }

  @Test
  void closedSessionIsReapedRatherThanMerelySkipped() throws Exception {
    var session = mock(WebSocketSession.class);
    when(session.getId()).thenReturn("zombie");
    when(session.isOpen()).thenReturn(false);

    handler.afterConnectionEstablished(session);
    clearInvocations(session);
    handler.broadcastEvent(PAYLOAD);

    verify(session, never()).sendMessage(any());

    // Reaped, not skipped: a later broadcast must not reach it even if the
    // session reports itself open again.
    when(session.isOpen()).thenReturn(true);
    handler.broadcastEvent(PAYLOAD);

    verify(session, never()).sendMessage(any());
  }

  private WebSocketSession openSession(String id) {
    var session = mock(WebSocketSession.class);
    when(session.getId()).thenReturn(id);
    when(session.isOpen()).thenReturn(true);
    return session;
  }

  private String sentPayload(WebSocketSession session) throws Exception {
    var captor = ArgumentCaptor.forClass(TextMessage.class);
    verify(session).sendMessage(captor.capture());
    return captor.getValue().getPayload();
  }
}
