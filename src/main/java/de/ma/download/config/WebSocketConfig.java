// File: WebSocketConfig.java
package de.ma.download.config;

import de.ma.download.security.KeycloakJwtAuthenticationConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.socket.config.annotation.*;

import java.util.Set;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    public static final String WEBSOCKET_ENDPOINT = "/ws";

    @Value("${app.allowed-origins:http://localhost:4200}")
    private Set<String> allowedOrigins;

    private final JwtDecoder jwtDecoder;
    private final KeycloakJwtAuthenticationConverter jwtAuthenticationConverter;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(WEBSOCKET_ENDPOINT)
                .addInterceptors(new JwtHandshakeInterceptor(jwtDecoder, jwtAuthenticationConverter))
                .setAllowedOrigins(allowedOrigins.toArray(String[]::new));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }
}
