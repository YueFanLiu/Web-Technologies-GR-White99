package fr.isep.projectweb.controller;

import fr.isep.projectweb.model.dto.request.UpdateMyProfileRequest;
import fr.isep.projectweb.model.dto.response.PublicUserResponse;
import fr.isep.projectweb.model.dto.response.UserProfileResponse;
import fr.isep.projectweb.model.service.FriendService;
import fr.isep.projectweb.model.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Business user profile endpoints")
public class UserController {

    private final UserService userService;
    private final FriendService friendService;

    public UserController(UserService userService, FriendService friendService) {
        this.userService = userService;
        this.friendService = friendService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current authenticated user's business profile")
    public UserProfileResponse getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return userService.getMyProfile(jwt);
    }

    @PutMapping("/me")
    @Operation(summary = "Update the current authenticated user's business profile")
    public UserProfileResponse updateMyProfile(@AuthenticationPrincipal Jwt jwt,
                                               @RequestBody UpdateMyProfileRequest request) {
        return userService.updateMyProfile(jwt, request);
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload the current authenticated user's avatar to Supabase Storage")
    public UserProfileResponse uploadMyAvatar(@AuthenticationPrincipal Jwt jwt,
                                              @RequestParam("file") MultipartFile file,
                                              @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        return userService.uploadMyAvatar(jwt, file, authorizationHeader);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a public user profile by id")
    public PublicUserResponse getUserById(@PathVariable UUID id) {
        return userService.getPublicUserById(id);
    }

    @GetMapping("/search")
    @Operation(summary = "Search public users by full name")
    public java.util.List<PublicUserResponse> searchUsers(@RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false) Integer limit,
                                                          @AuthenticationPrincipal Jwt jwt) {
        return friendService.searchUsers(keyword, limit, jwt);
    }
}
