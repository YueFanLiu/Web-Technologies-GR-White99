package fr.isep.projectweb.controller;

import fr.isep.projectweb.model.dto.request.RegistrationRequest;
import fr.isep.projectweb.model.dto.response.RegistrationResponse;
import fr.isep.projectweb.model.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/registrations")
@Tag(name = "Registrations", description = "CRUD endpoints for event registrations")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    @Operation(summary = "Create a registration for an event")
    public RegistrationResponse createRegistration(@RequestBody RegistrationRequest request,
                                                   @AuthenticationPrincipal Jwt jwt) {
        return registrationService.createRegistration(request, jwt);
    }

    @GetMapping
    @Operation(summary = "Get all registrations")
    public List<RegistrationResponse> getAllRegistrations() {
        return registrationService.getAllRegistrations();
    }

    @GetMapping("/event/{eventId}")
    @Operation(summary = "Get registrations by event id")
    public List<RegistrationResponse> getRegistrationsByEventId(@PathVariable UUID eventId) {
        return registrationService.getRegistrationsByEventId(eventId);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get registrations by user id")
    public List<RegistrationResponse> getRegistrationsByUserId(@PathVariable UUID userId) {
        return registrationService.getRegistrationsByUserId(userId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one registration by id")
    public RegistrationResponse getRegistrationById(@PathVariable UUID id) {
        return registrationService.getRegistrationById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a registration")
    public RegistrationResponse updateRegistration(@PathVariable UUID id, @RequestBody RegistrationRequest request) {
        return registrationService.updateRegistration(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a registration")
    public ResponseEntity<Void> deleteRegistration(@PathVariable UUID id) {
        registrationService.deleteRegistration(id);
        return ResponseEntity.noContent().build();
    }
}
