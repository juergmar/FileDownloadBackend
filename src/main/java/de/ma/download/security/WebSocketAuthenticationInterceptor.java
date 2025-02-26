package de.ma.download.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthenticationInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                Authentication auth = (Authentication) Objects.requireNonNull(accessor.getSessionAttributes()).get("_auth");
                if (auth != null) {
                    accessor.setUser(auth);
                    log.debug("Setting authentication for WebSocket connection: {}", auth.getName());
                }
            } else if (accessor.getMessageType() == SimpMessageType.MESSAGE ||
                    accessor.getMessageType() == SimpMessageType.SUBSCRIBE) {
                Principal user = accessor.getUser();
                if (user == null) {
                    Object auth = accessor.getSessionAttributes().get("_auth");
                    if (auth instanceof Authentication) {
                        accessor.setUser((Authentication) auth);
                        log.debug("Restored authentication for WebSocket message: {}", ((Authentication) auth).getName());
                    } else {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        if (authentication != null && authentication.isAuthenticated()) {
                            accessor.setUser(authentication);
                            log.debug("Set authentication from security context: {}", authentication.getName());
                        } else {
                            log.warn("No authenticated user found for WebSocket message");
                        }
                    }
                } else {
                    log.debug("User already present in message: {}", user.getName());
                }
            }
        }

        return message;
    }
}
