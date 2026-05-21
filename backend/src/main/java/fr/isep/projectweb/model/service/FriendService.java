package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.FriendRequestRepository;
import fr.isep.projectweb.model.dao.UserRepository;
import fr.isep.projectweb.model.dto.request.FriendRequestCreateRequest;
import fr.isep.projectweb.model.dto.response.FriendRequestResponse;
import fr.isep.projectweb.model.dto.response.FriendResponse;
import fr.isep.projectweb.model.dto.response.PublicUserResponse;
import fr.isep.projectweb.model.entity.FriendRequest;
import fr.isep.projectweb.model.entity.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class FriendService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final int DEFAULT_USER_SEARCH_LIMIT = 20;
    private static final int MAX_USER_SEARCH_LIMIT = 50;

    private final FriendRequestRepository friendRequestRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    public FriendService(FriendRequestRepository friendRequestRepository,
                         UserRepository userRepository,
                         CurrentUserService currentUserService,
                         NotificationService notificationService) {
        this.friendRequestRepository = friendRequestRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
    }

    public List<PublicUserResponse> searchUsers(String keyword, Integer limit, Jwt jwt) {
        UUID currentUserId = currentUserService.getCurrentUserId(jwt);
        String normalizedKeyword = normalizeOptional(keyword);
        return userRepository.searchPublicUsers(normalizedKeyword, PageRequest.of(0, normalizeUserSearchLimit(limit)))
                .stream()
                .filter(user -> !Objects.equals(user.getId(), currentUserId))
                .map(ResponseMapper::toPublicUserResponse)
                .toList();
    }

    public FriendRequestResponse createRequest(FriendRequestCreateRequest request, Jwt jwt) {
        User requester = currentUserService.getOrCreateCurrentUser(jwt);
        UUID addresseeId = request.getAddresseeId();
        if (addresseeId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Addressee id must not be null");
        }
        if (Objects.equals(requester.getId(), addresseeId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot send a friend request to yourself");
        }

        User addressee = findUser(addresseeId);
        List<FriendRequest> existingRequests = friendRequestRepository.findLatestBetweenUsers(
                requester.getId(),
                addresseeId,
                PageRequest.of(0, 1)
        );
        if (!existingRequests.isEmpty()) {
            FriendRequest existing = existingRequests.get(0);
            if (STATUS_PENDING.equals(existing.getStatus()) || STATUS_ACCEPTED.equals(existing.getStatus())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Friend request or friendship already exists");
            }
        }

        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setRequester(requester);
        friendRequest.setAddressee(addressee);
        friendRequest.setStatus(STATUS_PENDING);

        try {
            FriendRequest savedRequest = friendRequestRepository.save(friendRequest);
            notificationService.create(
                    addressee,
                    requester,
                    NotificationService.FRIEND_REQUEST_RECEIVED,
                    "New friend request",
                    requester.getFullName() + " sent you a friend request",
                    "FRIEND_REQUEST",
                    savedRequest.getId(),
                    "FRIEND_REQUEST",
                    savedRequest.getId(),
                    "friend_request:" + savedRequest.getId() + ":received",
                    java.util.Map.of("requestId", savedRequest.getId().toString())
            );
            return toFriendRequestResponse(savedRequest);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Friend request or friendship already exists");
        }
    }

    public List<FriendRequestResponse> getIncomingRequests(Jwt jwt) {
        UUID currentUserId = currentUserService.getCurrentUserId(jwt);
        return friendRequestRepository.findByAddresseeIdAndStatusOrderByCreatedAtDesc(currentUserId, STATUS_PENDING)
                .stream()
                .map(this::toFriendRequestResponse)
                .toList();
    }

    public List<FriendRequestResponse> getOutgoingRequests(Jwt jwt) {
        UUID currentUserId = currentUserService.getCurrentUserId(jwt);
        return friendRequestRepository.findByRequesterIdAndStatusOrderByCreatedAtDesc(currentUserId, STATUS_PENDING)
                .stream()
                .map(this::toFriendRequestResponse)
                .toList();
    }

    public List<FriendResponse> getFriends(Jwt jwt) {
        UUID currentUserId = currentUserService.getCurrentUserId(jwt);
        return friendRequestRepository.findAcceptedByUserId(currentUserId)
                .stream()
                .map(friendRequest -> toFriendResponse(friendRequest, currentUserId))
                .toList();
    }

    public FriendRequestResponse acceptRequest(UUID requestId, Jwt jwt) {
        User currentUser = currentUserService.getOrCreateCurrentUser(jwt);
        FriendRequest friendRequest = findRequest(requestId);
        ensureAddressee(friendRequest, currentUser.getId());
        ensurePending(friendRequest);
        friendRequest.setStatus(STATUS_ACCEPTED);
        friendRequest.setUpdatedAt(LocalDateTime.now());
        FriendRequest savedRequest = friendRequestRepository.save(friendRequest);
        notificationService.create(
                savedRequest.getRequester(),
                currentUser,
                NotificationService.FRIEND_REQUEST_ACCEPTED,
                "Friend request accepted",
                currentUser.getFullName() + " accepted your friend request",
                "USER",
                currentUser.getId(),
                "FRIEND_REQUEST",
                savedRequest.getId(),
                "friend_request:" + savedRequest.getId() + ":accepted",
                java.util.Map.of("requestId", savedRequest.getId().toString(), "friendUserId", currentUser.getId().toString())
        );
        return toFriendRequestResponse(savedRequest);
    }

    public FriendRequestResponse rejectRequest(UUID requestId, Jwt jwt) {
        UUID currentUserId = currentUserService.getCurrentUserId(jwt);
        FriendRequest friendRequest = findRequest(requestId);
        ensureAddressee(friendRequest, currentUserId);
        ensurePending(friendRequest);
        friendRequest.setStatus(STATUS_REJECTED);
        friendRequest.setUpdatedAt(LocalDateTime.now());
        FriendRequest savedRequest = friendRequestRepository.save(friendRequest);
        notificationService.create(
                savedRequest.getRequester(),
                savedRequest.getAddressee(),
                NotificationService.FRIEND_REQUEST_REJECTED,
                "Friend request declined",
                savedRequest.getAddressee().getFullName() + " declined your friend request",
                "FRIEND_REQUEST",
                savedRequest.getId(),
                "FRIEND_REQUEST",
                savedRequest.getId(),
                "friend_request:" + savedRequest.getId() + ":rejected",
                java.util.Map.of("requestId", savedRequest.getId().toString())
        );
        return toFriendRequestResponse(savedRequest);
    }

    public void cancelRequest(UUID requestId, Jwt jwt) {
        UUID currentUserId = currentUserService.getCurrentUserId(jwt);
        FriendRequest friendRequest = findRequest(requestId);
        if (!Objects.equals(friendRequest.getRequester().getId(), currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the requester can cancel this request");
        }
        ensurePending(friendRequest);
        friendRequest.setStatus(STATUS_CANCELLED);
        friendRequest.setUpdatedAt(LocalDateTime.now());
        friendRequestRepository.save(friendRequest);
    }

    public void removeFriend(UUID friendUserId, Jwt jwt) {
        UUID currentUserId = currentUserService.getCurrentUserId(jwt);
        FriendRequest friendRequest = friendRequestRepository.findAcceptedBetweenUsers(currentUserId, friendUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friendship not found"));
        friendRequest.setStatus(STATUS_CANCELLED);
        friendRequest.setUpdatedAt(LocalDateTime.now());
        friendRequestRepository.save(friendRequest);
    }

    public boolean areFriends(UUID firstUserId, UUID secondUserId) {
        return friendRequestRepository.findAcceptedBetweenUsers(firstUserId, secondUserId).isPresent();
    }

    private FriendRequest findRequest(UUID requestId) {
        return friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend request not found"));
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void ensureAddressee(FriendRequest friendRequest, UUID currentUserId) {
        if (!Objects.equals(friendRequest.getAddressee().getId(), currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the addressee can manage this request");
        }
    }

    private void ensurePending(FriendRequest friendRequest) {
        if (!STATUS_PENDING.equals(friendRequest.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Friend request is not pending");
        }
    }

    private FriendResponse toFriendResponse(FriendRequest friendRequest, UUID currentUserId) {
        User friend = Objects.equals(friendRequest.getRequester().getId(), currentUserId)
                ? friendRequest.getAddressee()
                : friendRequest.getRequester();

        FriendResponse response = new FriendResponse();
        response.setUser(ResponseMapper.toPublicUserResponse(friend));
        response.setFriendsSince(friendRequest.getUpdatedAt() != null ? friendRequest.getUpdatedAt() : friendRequest.getCreatedAt());
        return response;
    }

    private FriendRequestResponse toFriendRequestResponse(FriendRequest friendRequest) {
        FriendRequestResponse response = new FriendRequestResponse();
        response.setId(friendRequest.getId());
        response.setRequester(ResponseMapper.toPublicUserResponse(friendRequest.getRequester()));
        response.setAddressee(ResponseMapper.toPublicUserResponse(friendRequest.getAddressee()));
        response.setStatus(friendRequest.getStatus());
        response.setCreatedAt(friendRequest.getCreatedAt());
        response.setUpdatedAt(friendRequest.getUpdatedAt());
        return response;
    }

    private int normalizeUserSearchLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_USER_SEARCH_LIMIT;
        }
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, MAX_USER_SEARCH_LIMIT);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
