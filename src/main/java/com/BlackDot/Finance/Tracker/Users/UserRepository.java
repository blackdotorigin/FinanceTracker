package com.BlackDot.Finance.Tracker.Users;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByIdAndActiveTrue(UUID id);

    Optional<User> findByUsernameAndActiveTrue(String username);
    
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);
}
