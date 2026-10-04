package com.BlackDot.Finance.Tracker.Users;

import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.BlackDot.Finance.Tracker.Auth.SelfOnly;
import com.BlackDot.Finance.Tracker.CustomException.ResourceNotFoundException;
import com.BlackDot.Finance.Tracker.Users.DTO.CreateUserRequest;
import com.BlackDot.Finance.Tracker.Users.DTO.UpdateUserRequest;
import com.BlackDot.Finance.Tracker.Users.DTO.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repo;
    private final PasswordEncoder encoder;

    @Transactional
    public UserResponse create(CreateUserRequest createUserRequest) {
        String email = createUserRequest.email().trim().toLowerCase();
        String username = createUserRequest.username().trim().toLowerCase();
        if (repo.existsByEmailIgnoreCase(email))
            throw new RuntimeException("Email already registered");
        if (repo.existsByUsernameIgnoreCase(username))
            throw new RuntimeException("Username already registered");

        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(encoder.encode(createUserRequest.password()));
        user.setFullName(createUserRequest.fullName().trim());
        if (createUserRequest.defaultCurrency() != null){ 
            user.setDefaultCurrency(createUserRequest.defaultCurrency());
        }
        return UserResponse.from(repo.save(user));
    }

    @SelfOnly
    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        return UserResponse.from(repo.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }

    @SelfOnly
    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest updateUserRequest) {
        User user = repo.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));;
        user.setFullName(updateUserRequest.fullName().trim());
        if (updateUserRequest.defaultCurrency() != null){
            user.setDefaultCurrency(updateUserRequest.defaultCurrency());
        } 
        return UserResponse.from(user);   // dirty checking saves it
    }

    @SelfOnly
    @Transactional
    public void delete(UUID id) {
        User user = repo.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setActive(false);     // soft delete
    }
}
