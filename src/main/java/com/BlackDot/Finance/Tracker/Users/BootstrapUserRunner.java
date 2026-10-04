package com.BlackDot.Finance.Tracker.Users;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BootstrapUserRunner implements ApplicationRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-user.email:}")
    private String email;

    @Value("${app.bootstrap-user.username:}")
    private String username;

    @Value("${app.bootstrap-user.password:}")
    private String password;

    @Value("${app.bootstrap-user.full-name:Sample User}")
    private String fullName;

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(email) && !StringUtils.hasText(username)
                && !StringUtils.hasText(password)) {
            log.info("Bootstrap user is not configured; skipping creation");
            return;
        }

        if (!StringUtils.hasText(email) || !StringUtils.hasText(username)
                || !StringUtils.hasText(password)) {
            throw new IllegalStateException(
                    "BOOTSTRAP_USER_EMAIL, BOOTSTRAP_USER_USERNAME, and "
                            + "BOOTSTRAP_USER_PASSWORD must all be set");
        }
        if (password.length() < 8 || password.length() > 72) {
            throw new IllegalStateException("Bootstrap user password must be 8 to 72 characters");
        }
        if (!StringUtils.hasText(fullName)) {
            throw new IllegalStateException("BOOTSTRAP_USER_FULL_NAME must not be blank");
        }

        String normalizedEmail = email.trim().toLowerCase();
        String normalizedUsername = username.trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)
                || userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            log.info("Bootstrap user email or username already exists; leaving it unchanged");
            return;
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setUsername(normalizedUsername);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(fullName.trim());
        userRepository.save(user);
        log.info("Created bootstrap user {}", normalizedEmail);
    }
}
