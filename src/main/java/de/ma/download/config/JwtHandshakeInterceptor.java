package de.ma.download.config;

import de.ma.download.security.KeycloakJwtAuthenticationConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final String TOKEN_QUERY_PARAM = "token";

    private final JwtDecoder jwtDecoder;
    private final KeycloakJwtAuthenticationConverter jwtAuthenticationConverter;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        // Extract token from query parameters
        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst(TOKEN_QUERY_PARAM);

        if (token != null && !token.isEmpty()) {
            try {
                Jwt jwt = jwtDecoder.decode(token);
                // Set the security context attribute which Spring Security will use
                attributes.put("SPRING_SECURITY_CONTEXT",
                        new SecurityContextImpl(jwtAuthenticationConverter.convert(jwt)));
                log.debug("JWT authentication set during handshake for user: {}", jwt.getSubject());
            } catch (Exception e) {
                log.warn("Failed to decode JWT token during handshake: {}", e.getMessage());
            }
        } else {
            log.warn("No JWT token found in query parameters");
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No post-handshake action needed.
    }
}
