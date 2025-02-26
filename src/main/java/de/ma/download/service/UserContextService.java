package de.ma.download.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.security.Principal;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserContextService {

    /**
     * Get current user ID from the security context
     */
    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return extractUserId(authentication);
    }

    /**
     * Get user ID from the provided principal
     * This overload is useful for WebSocket contexts where the security context might be different
     */
    public String getUserIdFromPrincipal(Principal principal) {
        if (principal == null) {
            log.warn("Null principal provided");
            return "anonymous";
        }

        if (principal instanceof Authentication authentication) {
            return extractUserId(authentication);
        }

        // For generic principals, use getName()
        log.debug("Using generic principal name: {}", principal.getName());
        return principal.getName();
    }

    /**
     * Extract user ID from various authentication types
     */
    private String extractUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("No authenticated user found in security context");
            return "anonymous";
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof DefaultOidcUser) {
            return ((DefaultOidcUser) principal).getSubject();
        } else if (principal instanceof JwtAuthenticationToken) {
            return (String) ((JwtAuthenticationToken) principal).getTokenAttributes().get("sub");
        } else if (principal instanceof String) {
            return (String) principal;
        } else if (principal instanceof Jwt jwt) {
            return jwt.getSubject();
        }

        return principal.toString();
    }

    /**
     * Check if current user is an admin
     */
    public boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return isUserAdmin(authentication);
    }

    /**
     * Check if provided principal has admin role
     */
    public boolean isPrincipalAdmin(Principal principal) {
        if (principal instanceof Authentication authentication) {
            return isUserAdmin(authentication);
        }
        return false;
    }

    /**
     * Check if authentication has admin role
     */
    private boolean isUserAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
