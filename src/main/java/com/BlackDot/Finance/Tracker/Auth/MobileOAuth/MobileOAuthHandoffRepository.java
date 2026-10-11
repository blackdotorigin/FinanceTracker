package com.BlackDot.Finance.Tracker.Auth.MobileOAuth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MobileOAuthHandoffRepository extends JpaRepository<MobileOAuthHandoff, UUID> {
    Optional<MobileOAuthHandoff> findByCodeHash(String codeHash);

    void deleteByExpiresAtBefore(Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update MobileOAuthHandoff handoff
            set handoff.consumedAt = :now
            where handoff.codeHash = :codeHash
                and handoff.codeChallenge = :codeChallenge
                and handoff.state = :state
                and handoff.consumedAt is null
                and handoff.expiresAt > :now
            """)
    int consume(
            @Param("codeHash") String codeHash,
            @Param("codeChallenge") String codeChallenge,
            @Param("state") String state,
            @Param("now") Instant now);
}
