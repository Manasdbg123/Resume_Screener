package com.resumeScreener.Controllers;

import com.resumeScreener.Services.AuthService;
import com.resumeScreener.dto.AuthResponse;
import com.resumeScreener.dto.LoginRequest;
import com.resumeScreener.dto.SignupRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/signup")
    public AuthResponse signup(@RequestBody SignupRequest request, HttpSession session) {
        try {
            return authService.signup(request, session);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request, HttpSession session) {
        try {
            return authService.login(request, session);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ex.getMessage());
        }
    }

    @PostMapping("/logout")
    public void logout(HttpSession session) {
        authService.logout(session);
    }

    @GetMapping("/me")
    public AuthResponse currentUser(HttpSession session) {
        return authService.currentUser(session);
    }
}
