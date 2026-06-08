package fr.isep.projectweb.controller;

import fr.isep.projectweb.model.dto.request.AdminUserStatusRequest;
import fr.isep.projectweb.model.dto.request.AdminUserUpdateRequest;
import fr.isep.projectweb.model.dto.response.AdminUserResponse;
import fr.isep.projectweb.model.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Admin users", description = "Admin user management endpoints")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @Operation(summary = "List and search users")
    public List<AdminUserResponse> listUsers(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String role,
                                             @RequestParam(required = false) String status,
                                             @AuthenticationPrincipal Jwt jwt) {
        return adminUserService.listUsers(keyword, role, status, jwt);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user profile, role, or status")
    public AdminUserResponse updateUser(@PathVariable UUID id,
                                        @RequestBody AdminUserUpdateRequest request,
                                        @AuthenticationPrincipal Jwt jwt) {
        return adminUserService.updateUser(id, request, jwt);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update user status")
    public AdminUserResponse updateStatus(@PathVariable UUID id,
                                          @RequestBody AdminUserStatusRequest request,
                                          @AuthenticationPrincipal Jwt jwt) {
        return adminUserService.updateStatus(id, request, jwt);
    }
}
