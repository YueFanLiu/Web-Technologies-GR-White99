package fr.isep.projectweb.controller;

import fr.isep.projectweb.model.dto.request.EventTicketTierRequest;
import fr.isep.projectweb.model.dto.response.EventTicketTierResponse;
import fr.isep.projectweb.model.service.EventTicketTierService;
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
@RequestMapping("/api/events/{eventId}/ticket-tiers")
@Tag(name = "Event ticket tiers", description = "Ticket tier endpoints for events")
public class EventTicketTierController {

    private final EventTicketTierService eventTicketTierService;

    public EventTicketTierController(EventTicketTierService eventTicketTierService) {
        this.eventTicketTierService = eventTicketTierService;
    }

    @GetMapping
    @Operation(summary = "List ticket tiers for an event")
    public List<EventTicketTierResponse> listTicketTiers(@PathVariable UUID eventId) {
        return eventTicketTierService.listTicketTiers(eventId);
    }

    @PostMapping
    @Operation(summary = "Create a ticket tier")
    public EventTicketTierResponse createTicketTier(@PathVariable UUID eventId,
                                                    @RequestBody EventTicketTierRequest request,
                                                    @AuthenticationPrincipal Jwt jwt) {
        return eventTicketTierService.createTicketTier(eventId, request, jwt);
    }

    @PutMapping("/{tierId}")
    @Operation(summary = "Update a ticket tier")
    public EventTicketTierResponse updateTicketTier(@PathVariable UUID eventId,
                                                    @PathVariable UUID tierId,
                                                    @RequestBody EventTicketTierRequest request,
                                                    @AuthenticationPrincipal Jwt jwt) {
        return eventTicketTierService.updateTicketTier(eventId, tierId, request, jwt);
    }

    @DeleteMapping("/{tierId}")
    @Operation(summary = "Delete or deactivate a ticket tier")
    public ResponseEntity<Void> deleteTicketTier(@PathVariable UUID eventId,
                                                 @PathVariable UUID tierId,
                                                 @AuthenticationPrincipal Jwt jwt) {
        eventTicketTierService.deleteTicketTier(eventId, tierId, jwt);
        return ResponseEntity.noContent().build();
    }
}
