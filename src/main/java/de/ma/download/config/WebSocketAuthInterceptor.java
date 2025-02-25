package de.ma.download.config;

import de.ma.download.security.KeycloakJwtAuthenticationConverter;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    public static final String AUTHORIZATION_PREFIX = "Bearer ";

    private final JwtDecoder jwtDecoder;

    private final KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @Override
    public Message<?> preSend(@NotNull Message<?> message, @NotNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (nonNull(accessor) && StompCommand.CONNECT.equals(accessor.getCommand())) {
            setSecurityContext(accessor);
        }

        return message;
    }

    private void setSecurityContext(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader(AUTHORIZATION);

        if (isEmpty(authHeaders)) {
            log.warn("No JWT token found in headers of the websocket CONNECT request");
            return;
        }

        try {
            String jwtToken = authHeaders.getFirst().replace(AUTHORIZATION_PREFIX, "");
            Jwt jwt = jwtDecoder.decode(jwtToken);
            accessor.setUser(keycloakJwtAuthenticationConverter.convert(jwt));
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
    }
}
