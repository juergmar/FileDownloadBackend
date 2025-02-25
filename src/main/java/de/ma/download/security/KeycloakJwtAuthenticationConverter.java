package de.ma.download.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtAuthenticationConverter jwtConverter;

    public KeycloakJwtAuthenticationConverter() {
        this.jwtConverter = new JwtAuthenticationConverter();
        this.jwtConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Standard JWT scopes converter
            JwtGrantedAuthoritiesConverter scopesConverter = new JwtGrantedAuthoritiesConverter();
            Collection<GrantedAuthority> scopeAuthorities = scopesConverter.convert(jwt);

            // Extract Keycloak roles
            List<GrantedAuthority> roleAuthorities = extractKeycloakRoles(jwt);

            // Combine both authorities
            return Stream.concat(
                    scopeAuthorities.stream(),
                    roleAuthorities.stream()
            ).collect(Collectors.toList());
        });
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        return jwtConverter.convert(jwt);
    }

    @SuppressWarnings("unchecked")
    private List<GrantedAuthority> extractKeycloakRoles(Jwt jwt) {
        try {
            // Extract realm roles
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                List<String> roles = (List<String>) realmAccess.get("roles");
                return roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                        .collect(Collectors.toList());
            }

            // If no realm roles, return empty list
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }
}
