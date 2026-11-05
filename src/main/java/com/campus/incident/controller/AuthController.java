package com.campus.incident.controller;

import com.campus.incident.entity.User;
import com.campus.incident.repository.UserRepository;
import com.campus.incident.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          JwtUtil jwtUtil,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    public static class RegisterRequest {
        public String name;
        public String email;
        public String password;
        public String role;
        public String department;
    }

    public static class LoginRequest {
        public String email;
        public String password;
    }

    public static class AuthResponse {
        public String token;
        public String email;
        public String role;
        public String name;

        public AuthResponse(String token, String email, String role, String name) {
            this.token = token;
            this.email = email;
            this.role = role;
            this.name = name;
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.email)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
        }

        User.Role role = User.Role.STUDENT;
        if (request.role != null && !request.role.isEmpty()) {
            try {
                role = User.Role.valueOf(request.role.toUpperCase());
            } catch (IllegalArgumentException e) {
                role = User.Role.STUDENT;
            }
        }

        User user = User.builder()
            .name(request.name)
            .email(request.email)
            .password(passwordEncoder.encode(request.password))
            .role(role)
            .department(request.department)
            .build();

        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "User registered successfully", "email", user.getEmail(), "role", user.getRole()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email, request.password)
        );

        UserDetails userDetails = org.springframework.security.core.userdetails.User
            .withUsername(request.email)
            .password("")
            .roles("USER")
            .build();

        String jwt = jwtUtil.generateToken(userDetails);

        User user = userRepository.findByEmail(request.email)
            .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(new AuthResponse(jwt, user.getEmail(), user.getRole().name(), user.getName()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully. Clear token on client side."));
    }
}
