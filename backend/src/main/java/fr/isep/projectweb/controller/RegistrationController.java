package fr.isep.projectweb.controller;

import fr.isep.projectweb.model.dto.request.RegistrationRequest;
import fr.isep.projectweb.model.entity.Registration;
import fr.isep.projectweb.model.entity.User;
import fr.isep.projectweb.model.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public Registration createRegistration(@RequestBody RegistrationRequest request,
                                           @AuthenticationPrincipal User user) {
        return registrationService.createRegistration(request, user);
    }

    @GetMapping
    @Operation(summary = "Get all registrations")
    public List<Registration> getAllRegistrations() {
        return registrationService.getAllRegistrations();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one registration by id")
    public Registration getRegistrationById(@PathVariable UUID id) {
        return registrationService.getRegistrationById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a registration")
    public Registration updateRegistration(@PathVariable UUID id, @RequestBody RegistrationRequest request) {
        return registrationService.updateRegistration(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a registration")
    public ResponseEntity<Void> deleteRegistration(@PathVariable UUID id) {
        registrationService.deleteRegistration(id);
        return ResponseEntity.noContent().build();
    }
}
