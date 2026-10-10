package com.quizhub.controller;

import com.quizhub.dto.Dtos.*;
import com.quizhub.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/register") public AuthResponse register(@Valid @RequestBody RegisterRequest r) { return auth.register(r); }
    @PostMapping("/login")    public AuthResponse login(@Valid @RequestBody LoginRequest r) { return auth.login(r); }
}
