package com.BlackDot.Finance.Tracker.Auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import com.BlackDot.Finance.Tracker.AppProps;
import com.BlackDot.Finance.Tracker.Users.User;
import com.BlackDot.Finance.Tracker.Users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenService {
    private final JwtEncoder encoder;
    private final AppProps props;
    private final RefreshTokenRepository refreshTokens;
    private final UserRepository users;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public AuthTokens issue(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.jwt().issuer())
                .issuedAt(now)
                .expiresAt(now.plus(props.jwt().accessTtl()))
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .build();
        String access = encoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken token = new RefreshToken();
        token.setUserId(user.getId());
        token.setTokenHash(hash(raw));
        token.setExpiresAt(now.plus(props.jwt().refreshTtl()));
        refreshTokens.save(token);

        return new AuthTokens(access, raw, props.jwt().accessTtl().toSeconds());
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthTokens rotate(String raw) {
        Instant now = Instant.now();
        RefreshToken token = refreshTokens.findByTokenHash(hash(raw))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (token.getRevokedAt() != null) {
            refreshTokens.revokeAllForUser(token.getUserId(), now);
            throw new UnauthorizedException("Invalid refresh token");
        }
        if (token.getExpiresAt().isBefore(now)) {
            throw new UnauthorizedException("Refresh token expired");
        }

        User user = users.findByIdAndActiveTrue(token.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Account disabled"));
        token.setRevokedAt(now);
        return issue(user);
    }

    @Transactional
    public void revoke(String raw) {
        refreshTokens.findByTokenHash(hash(raw))
                .ifPresent(token -> token.setRevokedAt(Instant.now()));
    }

    private static String hash(String raw) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
