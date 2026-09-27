package com.ryanachten.ore.gateway.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class SocketConnectionHandlerTest {

  private static final String PAYLOAD = "{\"type\":\"sim.tick\",\"tick\":42}";

  private final SocketConnectionHandler handler = new SocketConnectionHandler();

  @Test
  void broadcastEventDeliversThePayloadToEveryConnectedSession() throws Exception {
    var first = openSession("first");
    var second = openSession("second");

    handler.afterConnectionEstablished(first);
    handler.afterConnectionEstablished(second);

    handler.broadcastEvent(PAYLOAD);

    assertThat(sentPayload(first)).isEqualTo(PAYLOAD);
    assertThat(sentPayload(second)).isEqualTo(PAYLOAD);
  }

  @Test
  void broadcastStillReachesHealthySessionsWhenOneFails() throws Exception {
    var failing = openSession("failing");
    var healthy = openSession("healthy");
    doThrow(new IOException("broken pipe")).when(failing).sendMessage(any());

    handler.afterConnectionEstablished(failing);
    handler.afterConnectionEstablished(healthy);

    handler.broadcastEvent(PAYLOAD);

    assertThat(sentPayload(healthy)).isEqualTo(PAYLOAD);
  }

  @Test
  void disconnectedSessionIsNoLongerWrittenTo() throws Exception {
    var session = openSession("gone");

    handler.afterConnectionEstablished(session);
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
