package com.BlackDot.Finance.Tracker.Users;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.BlackDot.Finance.Tracker.Auth.AuthorizationService;
import com.BlackDot.Finance.Tracker.Auth.CurrentUser;
import com.BlackDot.Finance.Tracker.Auth.SelfOrAdmin;
import com.BlackDot.Finance.Tracker.CustomException.BadRequestException;
import com.BlackDot.Finance.Tracker.CustomException.ConstraintViolationException;
import com.BlackDot.Finance.Tracker.CustomException.ResourceNotFoundException;
import com.BlackDot.Finance.Tracker.Roles.Roles;
import com.BlackDot.Finance.Tracker.Users.DTO.CreateUserRequest;
import com.BlackDot.Finance.Tracker.Users.DTO.UpdateUserRequest;
import com.BlackDot.Finance.Tracker.Users.DTO.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final AuthorizationService authservice;

    @Transactional
    public UserResponse create(CreateUserRequest createUserRequest) {
        String email = createUserRequest.email().trim().toLowerCase();
        String username = createUserRequest.username().trim().toLowerCase();
        if (repo.existsByEmailIgnoreCase(email))
            throw new ConstraintViolationException("Email already registered");
        if (repo.existsByUsernameIgnoreCase(username))
            throw new ConstraintViolationException("Username already registered");

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

    @SelfOrAdmin
    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        return UserResponse.from(repo.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }

    @SelfOrAdmin
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

    @SelfOrAdmin 
    @Transactional
    public void delete(UUID id) {
        User user = repo.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setActive(false);     // soft delete
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse changeRole(UUID userId, Roles newRole) {
        if (authservice.isSelf(userId)) {
            throw new BadRequestException("You cannot change your own role");
        }
        User user = repo.findByIdAndActiveTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Roles oldRole = user.getRole();
        user.setRole(newRole);
        log.info("ROLE_CHANGE adminId={} targetUserId={} {} -> {}",
                CurrentUser.id(), userId, oldRole, newRole);
        return UserResponse.from(user);
    }
}
