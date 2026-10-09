package com.BlackDot.Finance.Tracker.Auth;

import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import com.BlackDot.Finance.Tracker.Roles.Roles;

@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        AuthUser principal = AuthUser.fromClaims(
                UUID.fromString(jwt.getSubject()),
                jwt.getClaimAsString("email"),
                Roles.valueOf(jwt.getClaimAsString("role")));
        return new UsernamePasswordAuthenticationToken(principal, jwt, principal.getAuthorities());
    }
}
