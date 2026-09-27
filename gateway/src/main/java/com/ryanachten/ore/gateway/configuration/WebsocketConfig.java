package com.ryanachten.ore.gateway.configuration;

import com.ryanachten.ore.gateway.services.SocketConnectionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebsocketConfig implements WebSocketConfigurer {
  private final SocketConnectionHandler connectionHandler;

  public WebsocketConfig(SocketConnectionHandler connectionHandler) {
    this.connectionHandler = connectionHandler;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(connectionHandler, "/ws").setAllowedOrigins("*");
  }
}
