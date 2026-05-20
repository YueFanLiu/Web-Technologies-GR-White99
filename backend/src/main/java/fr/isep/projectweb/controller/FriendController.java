package fr.isep.projectweb.controller;

import fr.isep.projectweb.model.dto.request.FriendRequestCreateRequest;
import fr.isep.projectweb.model.dto.response.FriendRequestResponse;
import fr.isep.projectweb.model.dto.response.FriendResponse;
import fr.isep.projectweb.model.service.FriendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Friends", description = "Friend request and friendship endpoints")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @GetMapping("/api/friends")
    @Operation(summary = "Get the current authenticated user's friends")
    public List<FriendResponse> getFriends(@AuthenticationPrincipal Jwt jwt) {
        return friendService.getFriends(jwt);
    }

    @DeleteMapping("/api/friends/{friendUserId}")
    @Operation(summary = "Remove an accepted friend")
    public ResponseEntity<Void> removeFriend(@PathVariable UUID friendUserId,
                                             @AuthenticationPrincipal Jwt jwt) {
        friendService.removeFriend(friendUserId, jwt);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/friend-requests/incoming")
    @Operation(summary = "Get incoming pending friend requests")
    public List<FriendRequestResponse> getIncomingRequests(@AuthenticationPrincipal Jwt jwt) {
        return friendService.getIncomingRequests(jwt);
    }

    @GetMapping("/api/friend-requests/outgoing")
    @Operation(summary = "Get outgoing pending friend requests")
    public List<FriendRequestResponse> getOutgoingRequests(@AuthenticationPrincipal Jwt jwt) {
        return friendService.getOutgoingRequests(jwt);
    }

    @PostMapping("/api/friend-requests")
    @Operation(summary = "Send a friend request")
    public FriendRequestResponse createRequest(@RequestBody FriendRequestCreateRequest request,
                                               @AuthenticationPrincipal Jwt jwt) {
        return friendService.createRequest(request, jwt);
    }

    @PostMapping("/api/friend-requests/{requestId}/accept")
    @Operation(summary = "Accept an incoming friend request")
    public FriendRequestResponse acceptRequest(@PathVariable UUID requestId,
                                               @AuthenticationPrincipal Jwt jwt) {
        return friendService.acceptRequest(requestId, jwt);
    }

    @PostMapping("/api/friend-requests/{requestId}/reject")
    @Operation(summary = "Reject an incoming friend request")
    public FriendRequestResponse rejectRequest(@PathVariable UUID requestId,
                                               @AuthenticationPrincipal Jwt jwt) {
        return friendService.rejectRequest(requestId, jwt);
    }

    @DeleteMapping("/api/friend-requests/{requestId}")
    @Operation(summary = "Cancel an outgoing pending friend request")
    public ResponseEntity<Void> cancelRequest(@PathVariable UUID requestId,
                                              @AuthenticationPrincipal Jwt jwt) {
        friendService.cancelRequest(requestId, jwt);
        return ResponseEntity.noContent().build();
    }
}
