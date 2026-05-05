package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.UserRepository;
import fr.isep.projectweb.model.dto.request.UpdateMyProfileRequest;
import fr.isep.projectweb.model.dto.response.PublicUserResponse;
import fr.isep.projectweb.model.dto.response.UserProfileResponse;
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
    private final CurrentUserService currentUserService;
    private final SupabaseStorageService supabaseStorageService;

    public UserService(UserRepository userRepository,
                       CurrentUserService currentUserService,
                       SupabaseStorageService supabaseStorageService) {
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.supabaseStorageService = supabaseStorageService;
    }

    public UserProfileResponse getMyProfile(Jwt jwt) {
        return toUserProfileResponse(currentUserService.getOrCreateCurrentUser(jwt));
    }

    public UserProfileResponse updateMyProfile(Jwt jwt, UpdateMyProfileRequest request) {
        User user = currentUserService.getOrCreateCurrentUser(jwt);
        applyUpdate(user, request);
        return toUserProfileResponse(userRepository.save(user));
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
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Full name must not be blank");
        }

        user.setFullName(request.getFullName().trim());
        user.setPhone(normalizePhone(request.getPhone()));
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
