package com.BlackDot.Finance.Tracker.Auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import com.BlackDot.Finance.Tracker.Roles.Roles;
import com.BlackDot.Finance.Tracker.Users.User;
import com.BlackDot.Finance.Tracker.Users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuthUserService {
    private final UserRepository users;
    private final UserIdentityRepository identities;
    private final RefreshTokenRepository refreshTokens;

    @Transactional
    public User findOrCreate(OAuthProfile profile) {
        Optional<UserIdentity> existing = identities.findByProviderAndProviderUserId(
                profile.provider(), profile.providerUserId());
        if (existing.isPresent()) {
            return users.findByIdAndActiveTrue(existing.get().getUserId())
                    .orElseThrow(() -> error("account_disabled"));
        }

        if (profile.email() == null || !profile.emailVerified()) {
            throw error("email_not_verified");
        }
        String email = profile.email().trim().toLowerCase();

        User user = users.findByEmailIgnoreCase(email).orElseGet(() -> {
            User created = new User();
            created.setEmail(email);
            created.setUsername("oauth-" + UUID.randomUUID());
            created.setFullName(profile.name() != null ? profile.name() : email);
            created.setEmailVerified(true);
            created.setRole(Roles.USER);
            return users.save(created);
        });
        if (!user.isActive()) {
            throw error("account_disabled");
        }

        if (!user.isEmailVerified()) {
            user.setPasswordHash(null);
            user.setEmailVerified(true);
            refreshTokens.revokeAllForUser(user.getId(), Instant.now());
        }

        UserIdentity identity = new UserIdentity();
        identity.setUserId(user.getId());
        identity.setProvider(profile.provider());
        identity.setProviderUserId(profile.providerUserId());
        identities.save(identity);
        return user;
    }

    private OAuth2AuthenticationException error(String code) {
        return new OAuth2AuthenticationException(new OAuth2Error(code));
    }
}
