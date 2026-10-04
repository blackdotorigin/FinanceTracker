package com.BlackDot.Finance.Tracker.Users;

import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.BlackDot.Finance.Tracker.Auth.CurrentUser;
import com.BlackDot.Finance.Tracker.Users.DTO.CreateUserRequest;
import com.BlackDot.Finance.Tracker.Users.DTO.UpdateUserRequest;
import com.BlackDot.Finance.Tracker.Users.DTO.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest createUserRequest) {
        UserResponse created = userService.create(createUserRequest);
        return ResponseEntity.created(URI.create("/api/v1/users/" + created.id())).body(created);
    }

    @GetMapping("/me")
    public UserResponse me() { 
        return userService.get(CurrentUser.id()); 
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable UUID id) { 
        return userService.get(id); 
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest r) {
        return userService.update(id, r);
    }
 
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { 
        userService.delete(id); 
    }
}
