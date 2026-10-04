package com.BlackDot.Finance.Tracker.Auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {
    private CurrentUser() {}

    public static Optional<AuthUser> get() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .filter(AuthUser.class::isInstance)
                .map(AuthUser.class::cast);
    }

    public static UUID id() {
        return get().map(AuthUser::getId)
                .orElseThrow(() -> new AccessDeniedException("Not authenticated"));
    }
}
