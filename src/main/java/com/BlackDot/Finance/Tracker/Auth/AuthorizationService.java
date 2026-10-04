package com.BlackDot.Finance.Tracker.Auth;

import java.util.UUID;
import org.springframework.stereotype.Component;

// security/AuthorizationService.java
@Component("auth")
public class AuthorizationService {

    public boolean isSelf(UUID requestedId) {
        return CurrentUser.get().map(u -> u.getId().equals(requestedId)).orElse(false);
    }

    public boolean isOwner(Object obj) {
        if (obj instanceof OwnedEntity e) return isSelf(e.getOwnerId());
        return false;   // deny by default
    }
}
