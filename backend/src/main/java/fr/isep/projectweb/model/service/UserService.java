package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.AccessibilityPreferenceRepository;
import fr.isep.projectweb.model.dao.UserRepository;
import fr.isep.projectweb.model.dto.request.AccessibilityPreferencesRequest;
import fr.isep.projectweb.model.dto.request.UpdateMyProfileRequest;
import fr.isep.projectweb.model.dto.response.AccessibilityPreferencesResponse;
import fr.isep.projectweb.model.dto.response.PublicUserResponse;
import fr.isep.projectweb.model.dto.response.UserProfileResponse;
import fr.isep.projectweb.model.entity.AccessibilityPreference;
import fr.isep.projectweb.model.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AccessibilityPreferenceRepository accessibilityPreferenceRepository;
    private final CurrentUserService currentUserService;
    private final SupabaseStorageService supabaseStorageService;

    public UserService(UserRepository userRepository,
                       AccessibilityPreferenceRepository accessibilityPreferenceRepository,
                       CurrentUserService currentUserService,
                       SupabaseStorageService supabaseStorageService) {
        this.userRepository = userRepository;
        this.accessibilityPreferenceRepository = accessibilityPreferenceRepository;
        this.currentUserService = currentUserService;
        this.supabaseStorageService = supabaseStorageService;
    }

    public UserProfileResponse getMyProfile(Jwt jwt) {
        return toUserProfileResponse(currentUserService.getOrCreateCurrentUser(jwt));
    }

    @Transactional
    public UserProfileResponse updateMyProfile(Jwt jwt, UpdateMyProfileRequest request) {
        User user = currentUserService.getOrCreateCurrentUser(jwt);
        applyUpdate(user, request);
        User savedUser = userRepository.save(user);
        applyAccessibilityPreferences(savedUser, request.getAccessibilityPreferences());
        return toUserProfileResponse(savedUser);
    }

    public UserProfileResponse uploadMyAvatar(Jwt jwt, MultipartFile file, String authorizationHeader) {
        User user = currentUserService.getOrCreateCurrentUser(jwt);
        String photoUrl = supabaseStorageService.uploadUserAvatar(user.getId(), file, authorizationHeader);
        user.setPhoto(photoUrl);
        return toUserProfileResponse(userRepository.save(user));
    }

    public PublicUserResponse getPublicUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return toPublicUserResponse(user);
    }

    private void applyUpdate(User user, UpdateMyProfileRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body must not be null");
        }
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Full name must not be blank");
        }

        user.setFullName(request.getFullName().trim());
        user.setPhone(normalizePhone(request.getPhone()));
    }

    private void applyAccessibilityPreferences(User user, AccessibilityPreferencesRequest request) {
        if (request == null) {
            return;
        }

        accessibilityPreferenceRepository.deleteByUserId(user.getId());
        accessibilityPreferenceRepository.flush();
        accessibilityPreferenceRepository.saveAll(toUserPreferences(user, request));
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }

        return phone.trim();
    }

    private UserProfileResponse toUserProfileResponse(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setPhone(user.getPhone());
        response.setPhoto(user.getPhoto());
        response.setRole(user.getRole());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        response.setAccessibilityPreferences(toAccessibilityPreferencesResponse(user));
        return response;
    }

    private AccessibilityPreferencesResponse toAccessibilityPreferencesResponse(User user) {
        return toAccessibilityPreferencesResponse(accessibilityPreferenceRepository.findByUserId(user.getId()));
    }

    private AccessibilityPreferencesResponse toAccessibilityPreferencesResponse(List<AccessibilityPreference> preferences) {
        Set<String> featureKeys = preferences.stream()
                .map(AccessibilityPreference::getFeatureKey)
                .collect(Collectors.toSet());

        AccessibilityPreferencesResponse response = new AccessibilityPreferencesResponse();
        response.setWheelchairAccessible(featureKeys.contains(AccessibilityFeatureKeys.WHEELCHAIR_ACCESSIBLE));
        response.setElevatorNeeded(featureKeys.contains(AccessibilityFeatureKeys.ELEVATOR));
        response.setAccessibleRestroom(featureKeys.contains(AccessibilityFeatureKeys.ACCESSIBLE_RESTROOM));
        response.setQuietEnvironment(featureKeys.contains(AccessibilityFeatureKeys.QUIET_ENVIRONMENT));
        response.setStepFreeAccess(featureKeys.contains(AccessibilityFeatureKeys.STEP_FREE_ACCESS));
        return response;
    }

    private List<AccessibilityPreference> toUserPreferences(User user, AccessibilityPreferencesRequest request) {
        return java.util.stream.Stream.of(
                        toUserPreference(user, AccessibilityFeatureKeys.WHEELCHAIR_ACCESSIBLE,
                                request.getWheelchairAccessible()),
                        toUserPreference(user, AccessibilityFeatureKeys.ELEVATOR,
                                request.getElevatorNeeded()),
                        toUserPreference(user, AccessibilityFeatureKeys.ACCESSIBLE_RESTROOM,
                                request.getAccessibleRestroom()),
                        toUserPreference(user, AccessibilityFeatureKeys.QUIET_ENVIRONMENT,
                                request.getQuietEnvironment()),
                        toUserPreference(user, AccessibilityFeatureKeys.STEP_FREE_ACCESS,
                                request.getStepFreeAccess())
                )
                .filter(preference -> preference != null)
                .toList();
    }

    private AccessibilityPreference toUserPreference(User user, String featureKey, Boolean enabled) {
        if (!Boolean.TRUE.equals(enabled)) {
            return null;
        }

        AccessibilityPreference preference = new AccessibilityPreference();
        preference.setUser(user);
        preference.setFeatureKey(featureKey);
        return preference;
    }

    private PublicUserResponse toPublicUserResponse(User user) {
        PublicUserResponse response = new PublicUserResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setPhoto(user.getPhoto());
        response.setRole(user.getRole());
        return response;
    }
}
