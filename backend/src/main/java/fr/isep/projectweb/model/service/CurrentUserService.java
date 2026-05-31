package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.UserRepository;
import fr.isep.projectweb.model.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CurrentUserService {

    private static final String DEFAULT_ROLE = "PARENT";
    private static final Set<String> SELF_REGISTRATION_ROLES = Set.of("PARENT", "ORGANIZER");

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser(Jwt jwt) {
        return findCurrentUserByEmail(jwt)
                .or(() -> userRepository.findById(getCurrentUserId(jwt)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found"));
    }

    public User getOrCreateCurrentUser(Jwt jwt) {
        UUID userId = getCurrentUserId(jwt);
        return findCurrentUserByEmail(jwt)
                .or(() -> userRepository.findById(userId))
                .orElseGet(() -> userRepository.save(buildUserFromJwt(jwt, userId)));
    }

    private java.util.Optional<User> findCurrentUserByEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            return java.util.Optional.empty();
        }

        return userRepository.findFirstByEmailIgnoreCase(email.trim());
    }

    public UUID getCurrentUserId(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT subject is missing");
        }

        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT subject is invalid");
        }
    }

    private User buildUserFromJwt(Jwt jwt, UUID userId) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JWT email claim is missing");
        }

        User user = new User();
        user.setId(userId);
        user.setEmail(email.trim());
        user.setFullName(resolveFullName(jwt));
        user.setPhone(null);
        user.setRole(resolveRole(jwt));
        return user;
    }

    private String resolveFullName(Jwt jwt) {
        Object metadataClaim = jwt.getClaims().get("user_metadata");
        if (metadataClaim instanceof Map<?, ?> metadata) {
            Object fullName = metadata.get("full_name");
            if (fullName instanceof String value && !value.isBlank()) {
                return value.trim();
            }
        }

        return "";
    }

    private String resolveRole(Jwt jwt) {
        Object metadataClaim = jwt.getClaims().get("user_metadata");
        if (metadataClaim instanceof Map<?, ?> metadata) {
            Object role = metadata.get("role");
            if (role instanceof String value) {
                String normalized = value.trim().toUpperCase();
                if (SELF_REGISTRATION_ROLES.contains(normalized)) {
                    return normalized;
                }
            }
        }

        return DEFAULT_ROLE;
    }
}
