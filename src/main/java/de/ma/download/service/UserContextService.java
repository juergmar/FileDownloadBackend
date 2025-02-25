package de.ma.download.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserContextService {

    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
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
        }else if (principal instanceof Jwt jwt){
            return jwt.getSubject();
        }

        return principal.toString();
    }

    public boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
