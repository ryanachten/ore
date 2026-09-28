package com.ryanachten.ore.gateway.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class SocketConnectionHandler extends TextWebSocketHandler {
  private static final Logger log = LoggerFactory.getLogger(SocketConnectionHandler.class);
  private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
  private static final int SEND_TIME_LIMIT_MS = 10 * 1000;
  private static final int BUFFER_SIZE_LIMIT_BYTES = 512 * 1024; // 512 KB

  @Override
  public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
    super.afterConnectionEstablished(session);

    var concurrentSession =
        new ConcurrentWebSocketSessionDecorator(
            session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT_BYTES);

    var sessionId = session.getId();
    sessions.put(sessionId, concurrentSession);

    log.info("Connection established for session: {}", sessionId);
  }

  @Override
  public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status)
      throws Exception {
    super.afterConnectionClosed(session, status);
    log.info("Connection closed for session {} with status {}", session.getId(), status);

    sessions.remove(session.getId());
  }

  public void broadcastEvent(String messagePayload) {
    var message = new TextMessage(messagePayload);

    sessions.forEach(
        (sessionId, session) -> {
          if (!session.isOpen()) {
            sessions.remove(sessionId);
            return;
          }
          try {
            session.sendMessage(message);
          } catch (Exception exception) {
            log.error(
                "Failed to broadcast message to session {} with exception {}",
                session.getId(),
                exception.getMessage());
            sessions.remove(sessionId);
          }
        });
  }
}
