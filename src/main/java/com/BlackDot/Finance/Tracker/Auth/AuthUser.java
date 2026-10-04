package com.BlackDot.Finance.Tracker.Auth;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.BlackDot.Finance.Tracker.Users.User;
import lombok.Getter;

// security/AuthUser.java: the principal
@Getter
public class AuthUser implements UserDetails {
    private final UUID id;
    private final String email;
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean active;

    public AuthUser(User u) {
        this.id = u.getId();
        this.email = u.getEmail();
        this.username = u.getUsername();
        this.password = u.getPasswordHash();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        this.active = u.isActive();
    }
    
    @Override 
    public String getUsername() { 
        return username; 
    }

    @Override 
    public String getPassword() { 
        return password; 
    }

    @Override 
    public boolean isEnabled(){
        return active;
    }
}