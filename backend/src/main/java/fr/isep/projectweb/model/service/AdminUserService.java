package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.UserRepository;
import fr.isep.projectweb.model.dto.request.AdminUserStatusRequest;
import fr.isep.projectweb.model.dto.request.AdminUserUpdateRequest;
import fr.isep.projectweb.model.dto.response.AdminUserResponse;
import fr.isep.projectweb.model.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminUserService {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final Set<String> ROLES = Set.of("PARENT", "ORGANIZER", "ADMIN");
    private static final Set<String> STATUSES = Set.of("ACTIVE", "DEACTIVATED");

    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public AdminUserService(UserRepository userRepository, CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    public List<AdminUserResponse> listUsers(String keyword, String role, String status, Jwt jwt) {
        ensureAdmin(currentUserService.getCurrentUser(jwt));
        String normalizedKeyword = normalizeOptional(keyword);
        String normalizedRole = normalizeRoleOrNull(role);
        String normalizedStatus = normalizeStatusOrNull(status);

        return userRepository.findAll()
                .stream()
                .filter(user -> normalizedKeyword == null
                        || contains(user.getEmail(), normalizedKeyword)
                        || contains(user.getFullName(), normalizedKeyword)
                        || contains(user.getPhone(), normalizedKeyword))
                .filter(user -> normalizedRole == null || normalizedRole.equalsIgnoreCase(user.getRole()))
                .filter(user -> normalizedStatus == null || normalizedStatus.equalsIgnoreCase(safeStatus(user)))
                .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    public AdminUserResponse updateUser(UUID id, AdminUserUpdateRequest request, Jwt jwt) {
        ensureAdmin(currentUserService.getCurrentUser(jwt));
        User user = findUser(id);
        if (request.getFullName() != null) {
            user.setFullName(normalizeRequired(request.getFullName(), "Full name cannot be empty"));
        }
        if (request.getPhone() != null) {
            user.setPhone(normalizeOptional(request.getPhone()));
        }
        if (request.getRole() != null) {
            user.setRole(normalizeRole(request.getRole()));
        }
        if (request.getStatus() != null) {
            user.setStatus(normalizeStatus(request.getStatus()));
        }
        return toResponse(userRepository.save(user));
    }

    public AdminUserResponse updateStatus(UUID id, AdminUserStatusRequest request, Jwt jwt) {
        ensureAdmin(currentUserService.getCurrentUser(jwt));
        User user = findUser(id);
        user.setStatus(normalizeStatus(request != null ? request.getStatus() : null));
        return toResponse(userRepository.save(user));
    }

    private AdminUserResponse toResponse(User user) {
        AdminUserResponse response = new AdminUserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());
        response.setStatus(safeStatus(user));
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }

    private User findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void ensureAdmin(User user) {
        if (user == null || !ADMIN_ROLE.equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can manage users");
        }
    }

    private String normalizeRole(String value) {
        String normalized = normalizeRequired(value, "Role is required").toUpperCase(Locale.ROOT);
        if (!ROLES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
        }
        return normalized;
    }

    private String normalizeRoleOrNull(String value) {
        return value == null || value.isBlank() ? null : normalizeRole(value);
    }

    private String normalizeStatus(String value) {
        String normalized = normalizeRequired(value, "Status is required").toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user status");
        }
        return normalized;
    }

    private String normalizeStatusOrNull(String value) {
        return value == null || value.isBlank() ? null : normalizeStatus(value);
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private String safeStatus(User user) {
        return user.getStatus() != null ? user.getStatus() : "ACTIVE";
    }
}
