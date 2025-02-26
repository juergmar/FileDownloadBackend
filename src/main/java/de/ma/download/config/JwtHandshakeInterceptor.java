package de.ma.download.config;

import de.ma.download.security.KeycloakJwtAuthenticationConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
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
    private static final String SECURITY_CONTEXT_ATTR = "SPRING_SECURITY_CONTEXT";

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
                AbstractAuthenticationToken authentication = jwtAuthenticationConverter.convert(jwt);

                if (authentication != null) {
                    // Create and set the security context attribute
                    SecurityContext securityContext = new SecurityContextImpl(authentication);
                    attributes.put(SECURITY_CONTEXT_ATTR, securityContext);

                    // Also store the authentication for WebSocket message handling
                    attributes.put("_auth", authentication);

                    log.debug("JWT authentication set during handshake for user: {}", jwt.getSubject());
                }
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
