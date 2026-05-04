package fr.isep.projectweb.controller;

import fr.isep.projectweb.model.dto.request.ForgotPasswordRequest;
import fr.isep.projectweb.model.dto.request.LoginRequest;
import fr.isep.projectweb.model.dto.request.SignupRequest;
import fr.isep.projectweb.model.dto.response.AuthResponse;
import fr.isep.projectweb.model.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "JWT debug endpoints for Supabase integration")
public class AuthController {

    private final String supabaseUrl;
    private final String supabasePublishableKey;
    private final AuthService authService;

    public AuthController(@Value("${supabase.url}") String supabaseUrl,
                          @Value("${supabase.publishable-key:}") String supabasePublishableKey,
                          AuthService authService) {
        this.supabaseUrl = supabaseUrl;
        this.supabasePublishableKey = supabasePublishableKey;
        this.authService = authService;
    }

    @GetMapping("/config")
    @Operation(summary = "Get public Supabase configuration for local test pages")
    public Map<String, Object> getPublicConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("supabaseUrl", supabaseUrl);
        config.put("supabasePublishableKey", supabasePublishableKey);
        config.put("configured", !supabasePublishableKey.isBlank());
        return config;
    }

    @PostMapping("/signup")
    @Operation(summary = "Register a user via backend-managed Supabase Auth")
    public AuthResponse signup(@RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in a user via backend-managed Supabase Auth")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Send a Supabase password reset email")
    public AuthResponse forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @GetMapping("/debug")
    @Operation(summary = "Inspect the verified JWT claims from the current bearer token")
    public Map<String, Object> debugJwt(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("subject", jwt.getSubject());
        payload.put("email", jwt.getClaimAsString("email"));
        payload.put("role", jwt.getClaimAsString("role"));
        payload.put("issuer", jwt.getIssuer() != null ? jwt.getIssuer().toString() : null);
        payload.put("expiresAt", jwt.getExpiresAt());
        payload.put("claims", jwt.getClaims());
        return payload;
    }
}
