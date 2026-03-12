package com.resumeScreener.Services;

import com.resumeScreener.Repositories.UserRepository;
import com.resumeScreener.dto.AuthResponse;
import com.resumeScreener.dto.LoginRequest;
import com.resumeScreener.dto.SignupRequest;
import com.resumeScreener.entities.AppUser;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    public static final String SESSION_USER_ID = "auth.userId";

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthResponse signup(SignupRequest request, HttpSession session) {
        String email = normalizeEmail(request.getEmail());
        String name = normalizeName(request.getName());
        String password = request.getPassword() == null ? "" : request.getPassword().trim();

        if (name.isBlank() || email.isBlank() || password.length() < 6) {
            throw new IllegalArgumentException("Name, email, and a password of at least 6 characters are required.");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        AppUser user = new AppUser();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(userRepository.count() == 0 ? "ADMIN" : "CANDIDATE");
        user = userRepository.save(user);

        session.setAttribute(SESSION_USER_ID, user.getUserId());
        return toResponse(user, true);
    }

    public AuthResponse login(LoginRequest request, HttpSession session) {
        String email = normalizeEmail(request.getEmail());
        String password = request.getPassword() == null ? "" : request.getPassword();

        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        session.setAttribute(SESSION_USER_ID, user.getUserId());
        return toResponse(user, true);
    }

    public AuthResponse currentUser(HttpSession session) {
        Object userId = session.getAttribute(SESSION_USER_ID);
        if (!(userId instanceof Long id)) {
            return anonymousResponse();
        }

        return userRepository.findById(id)
                .map(user -> toResponse(user, true))
                .orElseGet(this::anonymousResponse);
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    public AppUser requireAuthenticatedUser(HttpSession session) {
        Object userId = session.getAttribute(SESSION_USER_ID);
        if (!(userId instanceof Long id)) {
            throw new IllegalArgumentException("You must be logged in.");
        }

        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User session is invalid."));
    }

    private AuthResponse anonymousResponse() {
        AuthResponse response = new AuthResponse();
        response.setAuthenticated(false);
        response.setRole("ANONYMOUS");
        return response;
    }

    private AuthResponse toResponse(AppUser user, boolean authenticated) {
        AuthResponse response = new AuthResponse();
        response.setUserId(user.getUserId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setAuthenticated(authenticated);
        return response;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }
}
