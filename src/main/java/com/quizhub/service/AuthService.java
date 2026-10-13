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
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public AuthResponse register(RegisterRequest r) {
        String username = r.username().trim();
        String email = r.email().trim().toLowerCase();

        if (users.existsByUsername(username))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That username is taken. Choose another.");
        if (users.existsByEmail(email))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That email is already registered. Sign in instead.");

        User u = users.save(new User(username, email, encoder.encode(r.password()), "STUDENT"));
        return new AuthResponse(
                jwt.generate(u.getUsername(), u.getRole()),
                u.getUsername(),
                u.getEmail(),
                u.getRole(),
                u.getLevel(),
                u.getXp(),
                u.getRank()
        );
    }

    public AuthResponse login(LoginRequest r) {
        String identifier = r.username().trim();
        User u = users.findByUsername(identifier)
                .or(() -> users.findByEmail(identifier.toLowerCase()))
                .filter(x -> encoder.matches(r.password(), x.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Invalid username or password. Check your details and try again."));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(u.getRole());
        return new AuthResponse(
                jwt.generate(u.getUsername(), u.getRole()),
                u.getUsername(),
                u.getEmail(),
                u.getRole(),
                isAdmin ? 1 : u.getLevel(),
                isAdmin ? 0L : u.getXp(),
                isAdmin ? "Administrator" : u.getRank()
        );
    }

    public AuthResponse getMe(String username) {
        User u = users.findByUsername(username)
                .or(() -> users.findByEmail(username.toLowerCase()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(u.getRole());
        return new AuthResponse(
                null,
                u.getUsername(),
                u.getEmail(),
                u.getRole(),
                isAdmin ? 1 : u.getLevel(),
                isAdmin ? 0L : u.getXp(),
                isAdmin ? "Administrator" : u.getRank()
        );
    }
}
