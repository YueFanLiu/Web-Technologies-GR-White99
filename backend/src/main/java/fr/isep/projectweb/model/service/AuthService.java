package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dto.request.LoginRequest;
import fr.isep.projectweb.model.dto.request.ForgotPasswordRequest;
import fr.isep.projectweb.model.dto.request.SignupRequest;
import fr.isep.projectweb.model.dto.response.AuthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");
    private static final Set<String> SELF_REGISTRATION_ROLES = Set.of("PARENT", "ORGANIZER");

    private final SupabaseAuthService supabaseAuthService;
    private final JwtDecoder jwtDecoder;
    private final CurrentUserService currentUserService;

    public AuthService(SupabaseAuthService supabaseAuthService,
                       JwtDecoder jwtDecoder,
                       CurrentUserService currentUserService) {
        this.supabaseAuthService = supabaseAuthService;
        this.jwtDecoder = jwtDecoder;
        this.currentUserService = currentUserService;
    }

    public AuthResponse signup(SignupRequest request) {
        String email = normalizeAndValidateEmail(request.getEmail());
        String password = normalizeAndValidatePassword(request.getPassword());
        String fullName = normalizeAndValidateFullName(request.getFullName());
        String role = normalizeAndValidateRole(request.getRole());

        Map<String, Object> payload = supabaseAuthService.signup(email, password, fullName, role);
        return toSignupResponse(payload, email);
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeAndValidateEmail(request.getEmail());
        String password = normalizeAndValidateLoginPassword(request.getPassword());

        Map<String, Object> payload = supabaseAuthService.login(email, password);
        String accessToken = requiredString(payload.get("access_token"), "Supabase login response is missing access_token");

        Jwt jwt = jwtDecoder.decode(accessToken);
        currentUserService.getOrCreateCurrentUser(jwt);

        AuthResponse response = baseSuccessResponse("AUTHENTICATED", "Login successful");
        response.setEmail(email);
        response.setAccessToken(accessToken);
        response.setRefreshToken(optionalString(payload.get("refresh_token")));
        response.setExpiresIn(optionalInteger(payload.get("expires_in")));
        response.setUser(buildUserSummary(payload.get("user"), jwt));
        return response;
    }

    public AuthResponse forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeAndValidateEmail(request.getEmail());
        String redirectTo = normalizeOptionalUrl(request.getRedirectTo());

        supabaseAuthService.forgotPassword(email, redirectTo);

        AuthResponse response = baseSuccessResponse(
                "PASSWORD_RESET_EMAIL_SENT",
                "Password reset email sent"
        );
        response.setEmail(email);
        return response;
    }

    public AuthResponse verifyEmail(String tokenHash, String type) {
        String normalizedTokenHash = normalize(tokenHash);
        if (normalizedTokenHash == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token hash must not be blank");
        }

        String normalizedType = normalize(type);
        if (normalizedType == null) {
            normalizedType = "signup";
        }

        Map<String, Object> payload = supabaseAuthService.verifyEmail(normalizedTokenHash, normalizedType);
        String accessToken = requiredString(payload.get("access_token"), "Supabase verify response is missing access_token");

        Jwt jwt = jwtDecoder.decode(accessToken);
        currentUserService.getOrCreateCurrentUser(jwt);

        AuthResponse response = baseSuccessResponse("AUTHENTICATED", "Email verified successfully");
        response.setEmail(jwt.getClaimAsString("email"));
        response.setAccessToken(accessToken);
        response.setRefreshToken(optionalString(payload.get("refresh_token")));
        response.setExpiresIn(optionalInteger(payload.get("expires_in")));
        response.setUser(buildUserSummary(payload.get("user"), jwt));
        return response;
    }

    private AuthResponse toSignupResponse(Map<String, Object> payload, String email) {
        AuthResponse response;
        String accessToken = optionalString(payload.get("access_token"));
        if (accessToken != null) {
            Jwt jwt = jwtDecoder.decode(accessToken);
            currentUserService.getOrCreateCurrentUser(jwt);

            response = baseSuccessResponse("AUTHENTICATED", "Registration successful");
            response.setAccessToken(accessToken);
            response.setRefreshToken(optionalString(payload.get("refresh_token")));
            response.setExpiresIn(optionalInteger(payload.get("expires_in")));
            response.setUser(buildUserSummary(payload.get("user"), jwt));
        } else {
            response = baseSuccessResponse(
                    "PENDING_EMAIL_CONFIRMATION",
                    "Registration successful. Please verify your email before logging in."
            );
            response.setUser(buildUserSummary(payload.get("user"), null));
        }

        response.setEmail(email);
        return response;
    }

    private AuthResponse baseSuccessResponse(String status, String message) {
        AuthResponse response = new AuthResponse();
        response.setSuccess(true);
        response.setStatus(status);
        response.setMessage(message);
        return response;
    }

    private AuthResponse.UserSummary buildUserSummary(Object userObject, Jwt jwt) {
        AuthResponse.UserSummary userSummary = new AuthResponse.UserSummary();

        if (jwt != null) {
            userSummary.setId(jwt.getSubject());
            userSummary.setEmail(jwt.getClaimAsString("email"));
            userSummary.setFullName(resolveFullNameFromJwt(jwt));
            userSummary.setRole(resolveRoleFromJwt(jwt));
            return userSummary;
        }

        if (userObject instanceof Map<?, ?> userMap) {
            userSummary.setId(optionalString(userMap.get("id")));
            userSummary.setEmail(optionalString(userMap.get("email")));
            userSummary.setFullName(resolveFullNameFromUserMap(userMap));
            userSummary.setRole(resolveRoleFromUserMap(userMap));
            return userSummary;
        }

        return null;
    }

    private String resolveFullNameFromJwt(Jwt jwt) {
        Object metadataClaim = jwt.getClaims().get("user_metadata");
        if (metadataClaim instanceof Map<?, ?> metadata) {
            return optionalString(metadata.get("full_name"));
        }
        return null;
    }

    private String resolveRoleFromJwt(Jwt jwt) {
        Object metadataClaim = jwt.getClaims().get("user_metadata");
        if (metadataClaim instanceof Map<?, ?> metadata) {
            return optionalString(metadata.get("role"));
        }
        return null;
    }

    private String resolveFullNameFromUserMap(Map<?, ?> userMap) {
        Object metadataObject = userMap.get("user_metadata");
        if (metadataObject instanceof Map<?, ?> metadata) {
            return optionalString(metadata.get("full_name"));
        }
        return null;
    }

    private String resolveRoleFromUserMap(Map<?, ?> userMap) {
        Object metadataObject = userMap.get("user_metadata");
        if (metadataObject instanceof Map<?, ?> metadata) {
            return optionalString(metadata.get("role"));
        }
        return null;
    }

    private String normalizeAndValidateEmail(String email) {
        String normalized = normalize(email);
        if (normalized == null || !EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email format is invalid");
        }
        return normalized;
    }

    private String normalizeAndValidatePassword(String password) {
        String normalized = normalize(password);
        if (normalized == null || !PASSWORD_PATTERN.matcher(normalized).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must be at least 8 characters and include uppercase, lowercase, and a digit"
            );
        }
        return normalized;
    }

    private String normalizeAndValidateLoginPassword(String password) {
        String normalized = normalize(password);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must not be blank");
        }
        return normalized;
    }

    private String normalizeAndValidateFullName(String fullName) {
        String normalized = normalize(fullName);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Full name must not be blank");
        }
        return normalized;
    }

    private String normalizeAndValidateRole(String role) {
        String normalized = normalize(role);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must not be blank");
        }

        String upperCaseRole = normalized.toUpperCase();
        if (!SELF_REGISTRATION_ROLES.contains(upperCaseRole)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must be PARENT or ORGANIZER");
        }
        return upperCaseRole;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String normalizeOptionalUrl(String value) {
        return normalize(value);
    }

    private String optionalString(Object value) {
        if (value instanceof String string && !string.isBlank()) {
            return string;
        }
        return null;
    }

    private Integer optionalInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private String requiredString(Object value, String message) {
        String result = optionalString(value);
        if (result == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, message);
        }
        return result;
    }
}
