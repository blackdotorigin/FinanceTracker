package com.BlackDot.Finance.Tracker.Auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.BlackDot.Finance.Tracker.Users.UserRepository;

@Service
public class AppUserDetailsService implements UserDetailsService {
    private final UserRepository repo;

    public AppUserDetailsService(UserRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return repo.findByUsernameAndActiveTrue(username.trim().toLowerCase())
                .map(AuthUser::fromUser)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }
}