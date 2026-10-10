package com.quizhub.service;

import com.quizhub.config.JwtUtil;
import com.quizhub.dto.Dtos.*;
import com.quizhub.entity.User;
import com.quizhub.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtUtil jwt) {
        this.users = users; this.encoder = encoder; this.jwt = jwt;
    }

    public AuthResponse register(RegisterRequest r) {
        if (users.existsByUsername(r.username()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That username is taken. Choose another.");
        if (users.existsByEmail(r.email()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That email is already registered. Sign in instead.");
        User u = users.save(new User(r.username(), r.email(), encoder.encode(r.password()), "STUDENT"));
        return new AuthResponse(jwt.generate(u.getUsername(), u.getRole()), u.getUsername(), u.getRole());
    }

    public AuthResponse login(LoginRequest r) {
        User u = users.findByUsername(r.username())
                .filter(x -> encoder.matches(r.password(), x.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Invalid username or password. Check your details and try again."));
        return new AuthResponse(jwt.generate(u.getUsername(), u.getRole()), u.getUsername(), u.getRole());
    }
}
