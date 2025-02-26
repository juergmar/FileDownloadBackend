package de.ma.download.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Slf4j
public class PrincipalHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        // Try to get principal from attributes (set by JwtHandshakeInterceptor)
        Object securityContextAttr = attributes.get("SPRING_SECURITY_CONTEXT");
        if (securityContextAttr instanceof SecurityContext securityContext) {
            Authentication auth = securityContext.getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                log.debug("Using Authentication from SecurityContext: {}", auth.getName());
                return auth;
            }
        }

        // Try to get authentication directly from attributes
        Object authAttr = attributes.get("_auth");
        if (authAttr instanceof Authentication auth) {
            log.debug("Using Authentication from attributes: {}", auth.getName());
            return auth;
        }

        // Fallback to default behavior
        Principal principal = super.determineUser(request, wsHandler, attributes);
        if (principal != null) {
            log.debug("Using default principal: {}", principal.getName());
            return principal;
        }

        // Last resort: a temporary principal to avoid null
        // This should not happen if the handshake interceptor works correctly
        log.warn("No principal found, creating dummy principal");
        return new UsernamePasswordAuthenticationToken("anonymous", null);
    }
}
