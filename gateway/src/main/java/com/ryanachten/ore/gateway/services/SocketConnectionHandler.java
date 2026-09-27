package com.ryanachten.ore.gateway.services;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class SocketConnectionHandler extends TextWebSocketHandler {
  private static final Logger log = LoggerFactory.getLogger(SocketConnectionHandler.class);
  private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

  @Override
  public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
    super.afterConnectionEstablished(session);
    log.info("Connection established for session: {}", session.getId());

    sessions.add(session);
  }

  @Override
  public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status)
      throws Exception {
    super.afterConnectionClosed(session, status);
    log.info("Connection closed for session {} with status {}", session.getId(), status);

    sessions.remove(session);
  }

  public void broadcastEvent(String messagePayload) {
    var message = new TextMessage(messagePayload);
    for (var session : sessions) {
      if (session.isOpen()) {
        try {
          session.sendMessage(message);
        } catch (Exception exception) {
          log.error(
              "Failed to broadcast message to session {} with exception {}",
              session.getId(),
              exception.getMessage());
        }
      } else {
        sessions.remove(session);
      }
    }
  }
}
