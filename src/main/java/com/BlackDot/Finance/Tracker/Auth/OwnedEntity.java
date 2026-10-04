package com.BlackDot.Finance.Tracker.Auth;

import java.util.UUID;

// security/OwnedEntity.java: implement on every user-owned entity (Transaction, etc.)
public interface OwnedEntity {
    UUID getOwnerId();
}