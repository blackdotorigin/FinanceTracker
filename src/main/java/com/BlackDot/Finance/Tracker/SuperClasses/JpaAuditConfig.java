package com.BlackDot.Finance.Tracker.SuperClasses;

import java.util.Optional;
import com.BlackDot.Finance.Tracker.Auth.CurrentUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditConfig {
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> CurrentUser.get()
                .map(user -> user.getId().toString())
                .or(() -> Optional.of("system"));
    }
}