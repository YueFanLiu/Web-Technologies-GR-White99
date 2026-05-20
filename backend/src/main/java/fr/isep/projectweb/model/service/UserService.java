package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.UserAccessibilityPreferenceRepository;
import fr.isep.projectweb.model.dao.UserRepository;
import fr.isep.projectweb.model.dto.request.AccessibilityPreferencesRequest;
import fr.isep.projectweb.model.dto.request.UpdateMyProfileRequest;
import fr.isep.projectweb.model.dto.response.AccessibilityPreferencesResponse;
import fr.isep.projectweb.model.dto.response.PublicUserResponse;
import fr.isep.projectweb.model.dto.response.UserProfileResponse;
import fr.isep.projectweb.model.entity.UserAccessibilityPreference;
import fr.isep.projectweb.model.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserAccessibilityPreferenceRepository accessibilityPreferenceRepository;
    private final CurrentUserService currentUserService;
    private final SupabaseStorageService supabaseStorageService;

    public UserService(UserRepository userRepository,
                       UserAccessibilityPreferenceRepository accessibilityPreferenceRepository,
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

        UserAccessibilityPreference preference = accessibilityPreferenceRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserAccessibilityPreference newPreference = new UserAccessibilityPreference();
                    newPreference.setUser(user);
                    return newPreference;
                });

        preference.setWheelchairAccessible(Boolean.TRUE.equals(request.getWheelchairAccessible()));
        preference.setElevatorNeeded(Boolean.TRUE.equals(request.getElevatorNeeded()));
        preference.setAccessibleRestroom(Boolean.TRUE.equals(request.getAccessibleRestroom()));
        preference.setQuietEnvironment(Boolean.TRUE.equals(request.getQuietEnvironment()));
        accessibilityPreferenceRepository.save(preference);
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
        return accessibilityPreferenceRepository.findByUserId(user.getId())
                .map(this::toAccessibilityPreferencesResponse)
                .orElseGet(this::defaultAccessibilityPreferencesResponse);
    }

    private AccessibilityPreferencesResponse toAccessibilityPreferencesResponse(UserAccessibilityPreference preference) {
        AccessibilityPreferencesResponse response = new AccessibilityPreferencesResponse();
        response.setWheelchairAccessible(Boolean.TRUE.equals(preference.getWheelchairAccessible()));
        response.setElevatorNeeded(Boolean.TRUE.equals(preference.getElevatorNeeded()));
        response.setAccessibleRestroom(Boolean.TRUE.equals(preference.getAccessibleRestroom()));
        response.setQuietEnvironment(Boolean.TRUE.equals(preference.getQuietEnvironment()));
        return response;
    }

    private AccessibilityPreferencesResponse defaultAccessibilityPreferencesResponse() {
        AccessibilityPreferencesResponse response = new AccessibilityPreferencesResponse();
        response.setWheelchairAccessible(false);
        response.setElevatorNeeded(false);
        response.setAccessibleRestroom(false);
        response.setQuietEnvironment(false);
        return response;
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
