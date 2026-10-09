package com.BlackDot.Finance.Tracker.Auth;

import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("auth")
public class AuthorizationService {

    public boolean isSelf(UUID requestedId) {
        return CurrentUser.get().map(u -> u.getId().equals(requestedId)).orElse(false);
    }

    public boolean isAdmin() {
        return CurrentUser.get()
                .map(u -> u.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")))
                .orElse(false);
    }

    /** Owner, or an admin (admin access is logged). */
    public boolean canAccess(UUID ownerId) {
        if (isSelf(ownerId)) return true;
        if (isAdmin()) {
            log.info("ADMIN_ACCESS adminId={} targetUserId={}", CurrentUser.id(), ownerId);
            return true;
        }
        return false;
    }

    public void assertOwner(OwnedEntity e) {          // strict: owner only
        if (!isSelf(e.getOwnerId())) throw new AccessDeniedException("Access denied");
    }

    public void assertAccess(OwnedEntity e) {         // owner or admin
        if (!canAccess(e.getOwnerId())) throw new AccessDeniedException("Access denied");
    }
}
