package com.ryanachten.ore.gateway.services;

import com.ryanachten.ore.common.contracts.EventEnvelope;
import com.ryanachten.ore.common.contracts.EventType;
import com.ryanachten.ore.gateway.repositories.WorldStateRepository;
import java.util.Map;
import java.util.UUID;
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
import tools.jackson.databind.ObjectMapper;

@Component
public class SocketConnectionHandler extends TextWebSocketHandler {
  private static final Logger log = LoggerFactory.getLogger(SocketConnectionHandler.class);
  private static final int SEND_TIME_LIMIT_MS = 10 * 1000; // 1000 MS
  private static final int BUFFER_SIZE_LIMIT_BYTES = 512 * 1024; // 512 KB
  private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
  private final WorldStateRepository worldStateRepository;
  private final ObjectMapper objectMapper;

  public SocketConnectionHandler(
      WorldStateRepository worldStateRepository, ObjectMapper objectMapper) {
    this.worldStateRepository = worldStateRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
    super.afterConnectionEstablished(session);

    var concurrentSession =
        new ConcurrentWebSocketSessionDecorator(
            session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT_BYTES);

    var sessionId = session.getId();
    sessions.put(sessionId, concurrentSession);

    var currentState = worldStateRepository.getState();
    var event =
        new EventEnvelope(
            UUID.randomUUID(),
            EventType.CURRENT_WORLD_STATE,
            currentState.tick(),
            "gateway",
            1,
            Map.of("state", currentState));
    var eventPayload = objectMapper.writeValueAsString(event);
    concurrentSession.sendMessage(new TextMessage(eventPayload));

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
