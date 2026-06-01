package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.algorithm.recommendation.friend.FriendRecommendationFeatures;
import fr.isep.projectweb.model.algorithm.recommendation.friend.FriendRecommendationScorer;
import fr.isep.projectweb.model.dao.AccessibilityPreferenceRepository;
import fr.isep.projectweb.model.dao.EventReviewRepository;
import fr.isep.projectweb.model.dao.EventSaveRepository;
import fr.isep.projectweb.model.dao.FriendRequestRepository;
import fr.isep.projectweb.model.dao.PostReviewRepository;
import fr.isep.projectweb.model.dao.RegistrationRepository;
import fr.isep.projectweb.model.dao.UserRepository;
import fr.isep.projectweb.model.dto.request.FriendRequestCreateRequest;
import fr.isep.projectweb.model.dto.response.FriendRecommendationResponse;
import fr.isep.projectweb.model.dto.response.FriendRequestResponse;
import fr.isep.projectweb.model.dto.response.FriendResponse;
import fr.isep.projectweb.model.dto.response.PublicUserResponse;
import fr.isep.projectweb.model.entity.AccessibilityPreference;
import fr.isep.projectweb.model.entity.FriendRequest;
import fr.isep.projectweb.model.entity.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FriendService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final int DEFAULT_USER_SEARCH_LIMIT = 20;
    private static final int MAX_USER_SEARCH_LIMIT = 50;
    private static final int DEFAULT_RECOMMENDATION_LIMIT = 6;
    private static final int MAX_RECOMMENDATION_LIMIT = 20;
    private static final String WHEELCHAIR_ACCESSIBLE = "wheelchair_accessible";
    private static final String ELEVATOR = "elevator";
    private static final String ACCESSIBLE_RESTROOM = "accessible_restroom";
    private static final String QUIET_ENVIRONMENT = "quiet_environment";

    private final FriendRequestRepository friendRequestRepository;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final EventSaveRepository eventSaveRepository;
    private final EventReviewRepository eventReviewRepository;
    private final PostReviewRepository postReviewRepository;
    private final AccessibilityPreferenceRepository accessibilityPreferenceRepository;
    private final FriendRecommendationScorer friendRecommendationScorer;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    public FriendService(FriendRequestRepository friendRequestRepository,
                         UserRepository userRepository,
                         RegistrationRepository registrationRepository,
                         EventSaveRepository eventSaveRepository,
                         EventReviewRepository eventReviewRepository,
                         PostReviewRepository postReviewRepository,
                         AccessibilityPreferenceRepository accessibilityPreferenceRepository,
                         FriendRecommendationScorer friendRecommendationScorer,
                         CurrentUserService currentUserService,
                         NotificationService notificationService) {
        this.friendRequestRepository = friendRequestRepository;
        this.userRepository = userRepository;
        this.registrationRepository = registrationRepository;
        this.eventSaveRepository = eventSaveRepository;
        this.eventReviewRepository = eventReviewRepository;
        this.postReviewRepository = postReviewRepository;
        this.accessibilityPreferenceRepository = accessibilityPreferenceRepository;
        this.friendRecommendationScorer = friendRecommendationScorer;
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

    public List<FriendRecommendationResponse> getFriendRecommendations(Integer limit, Jwt jwt) {
        User currentUser = currentUserService.getOrCreateCurrentUser(jwt);
        UUID currentUserId = currentUser.getId();
        int normalizedLimit = normalizeRecommendationLimit(limit);

        Set<UUID> currentFriendIds = getFriendIds(currentUserId);
        Set<UUID> blockedUserIds = new HashSet<>();
        blockedUserIds.add(currentUserId);
        blockedUserIds.addAll(currentFriendIds);
        blockedUserIds.addAll(friendRequestRepository
                .findByRequesterIdAndStatusOrderByCreatedAtDesc(currentUserId, STATUS_PENDING)
                .stream()
                .map(request -> request.getAddressee().getId())
                .collect(Collectors.toSet()));
        blockedUserIds.addAll(friendRequestRepository
                .findByAddresseeIdAndStatusOrderByCreatedAtDesc(currentUserId, STATUS_PENDING)
                .stream()
                .map(request -> request.getRequester().getId())
                .collect(Collectors.toSet()));

        Set<UUID> currentEventIds = new HashSet<>(registrationRepository.findActiveEventIdsByUserId(currentUserId));
        Set<UUID> currentSavedEventIds = new HashSet<>(eventSaveRepository.findSavedEventIdsByUserId(currentUserId));
        Set<String> currentReviewTargets = getReviewTargets(currentUserId);
        Set<String> currentAccessibilityFeatures = getAccessibilityFeatures(currentUserId);

        return userRepository.findAll()
                .stream()
                .filter(candidate -> candidate.getId() != null && !blockedUserIds.contains(candidate.getId()))
                .map(candidate -> toFriendRecommendationResponse(
                        currentUser,
                        candidate,
                        currentFriendIds,
                        currentEventIds,
                        currentSavedEventIds,
                        currentReviewTargets,
                        currentAccessibilityFeatures
                ))
                .filter(recommendation -> recommendation.getScore() > 0)
                .sorted(Comparator
                        .comparingDouble(FriendRecommendationResponse::getScore).reversed()
                        .thenComparing(Comparator.comparingInt(FriendRecommendationResponse::getMutualFriendCount).reversed())
                        .thenComparing(Comparator.comparingInt(FriendRecommendationResponse::getSharedEventCount).reversed())
                        .thenComparing(recommendation -> normalizeSortName(recommendation.getUser())))
                .limit(normalizedLimit)
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

    private FriendRecommendationResponse toFriendRecommendationResponse(User currentUser,
                                                                        User candidate,
                                                                        Set<UUID> currentFriendIds,
                                                                        Set<UUID> currentEventIds,
                                                                        Set<UUID> currentSavedEventIds,
                                                                        Set<String> currentReviewTargets,
                                                                        Set<String> currentAccessibilityFeatures) {
        UUID candidateId = candidate.getId();
        Set<UUID> candidateFriendIds = getFriendIds(candidateId);
        Set<UUID> candidateEventIds = new HashSet<>(registrationRepository.findActiveEventIdsByUserId(candidateId));
        Set<UUID> candidateSavedEventIds = new HashSet<>(eventSaveRepository.findSavedEventIdsByUserId(candidateId));
        Set<String> candidateReviewTargets = getReviewTargets(candidateId);
        Set<String> candidateAccessibilityFeatures = getAccessibilityFeatures(candidateId);

        int mutualFriendCount = intersectionSize(currentFriendIds, candidateFriendIds);
        int sharedEventCount = intersectionSize(currentEventIds, candidateEventIds);
        int sharedSavedEventCount = intersectionSize(currentSavedEventIds, candidateSavedEventIds);
        int sharedReviewCount = intersectionSize(currentReviewTargets, candidateReviewTargets);

        FriendRecommendationFeatures features = new FriendRecommendationFeatures();
        features.setMutualFriendCount(mutualFriendCount);
        features.setSharedEventCount(sharedEventCount);
        features.setSharedSavedEventCount(sharedSavedEventCount);
        features.setSharedReviewTargetCount(sharedReviewCount);
        features.setWheelchairAccessibleMatch(hasSharedFeature(currentAccessibilityFeatures, candidateAccessibilityFeatures, WHEELCHAIR_ACCESSIBLE));
        features.setElevatorMatch(hasSharedFeature(currentAccessibilityFeatures, candidateAccessibilityFeatures, ELEVATOR));
        features.setAccessibleRestroomMatch(hasSharedFeature(currentAccessibilityFeatures, candidateAccessibilityFeatures, ACCESSIBLE_RESTROOM));
        features.setQuietEnvironmentMatch(hasSharedFeature(currentAccessibilityFeatures, candidateAccessibilityFeatures, QUIET_ENVIRONMENT));
        features.setCurrentUserRole(currentUser.getRole());
        features.setCandidateRole(candidate.getRole());
        features.setCandidateHasPhoto(hasText(candidate.getPhoto()));
        features.setCandidateHasFullName(hasText(candidate.getFullName()));

        FriendRecommendationResponse response = new FriendRecommendationResponse();
        response.setUser(ResponseMapper.toPublicUserResponse(candidate));
        response.setScore(friendRecommendationScorer.score(features));
        response.setMutualFriendCount(mutualFriendCount);
        response.setSharedEventCount(sharedEventCount);
        response.setSharedSavedEventCount(sharedSavedEventCount);
        response.setSharedReviewCount(sharedReviewCount);
        response.setAccessibilityMatchCount(accessibilityMatchCount(features));
        response.setReasons(recommendationReasons(response));
        return response;
    }

    private Set<UUID> getFriendIds(UUID userId) {
        return friendRequestRepository.findAcceptedByUserId(userId)
                .stream()
                .map(friendRequest -> Objects.equals(friendRequest.getRequester().getId(), userId)
                        ? friendRequest.getAddressee().getId()
                        : friendRequest.getRequester().getId())
                .collect(Collectors.toSet());
    }

    private Set<String> getReviewTargets(UUID userId) {
        Set<String> targets = new HashSet<>();
        eventReviewRepository.findReviewedEventIdsByUserId(userId)
                .forEach(eventId -> targets.add("event:" + eventId));
        postReviewRepository.findReviewedPostIdsByUserId(userId)
                .forEach(postId -> targets.add("post:" + postId));
        return targets;
    }

    private Set<String> getAccessibilityFeatures(UUID userId) {
        return accessibilityPreferenceRepository.findByUserId(userId)
                .stream()
                .map(AccessibilityPreference::getFeatureKey)
                .filter(this::hasText)
                .map(feature -> feature.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private int intersectionSize(Set<?> first, Set<?> second) {
        if (first.isEmpty() || second.isEmpty()) {
            return 0;
        }
        Set<?> smaller = first.size() <= second.size() ? first : second;
        Set<?> larger = first.size() <= second.size() ? second : first;
        int count = 0;
        for (Object value : smaller) {
            if (larger.contains(value)) {
                count++;
            }
        }
        return count;
    }

    private boolean hasSharedFeature(Set<String> currentFeatures, Set<String> candidateFeatures, String feature) {
        return currentFeatures.contains(feature) && candidateFeatures.contains(feature);
    }

    private int accessibilityMatchCount(FriendRecommendationFeatures features) {
        int count = 0;
        if (features.isWheelchairAccessibleMatch()) count++;
        if (features.isElevatorMatch()) count++;
        if (features.isAccessibleRestroomMatch()) count++;
        if (features.isQuietEnvironmentMatch()) count++;
        return count;
    }

    private List<String> recommendationReasons(FriendRecommendationResponse response) {
        List<String> reasons = new ArrayList<>();
        if (response.getMutualFriendCount() > 0) {
            reasons.add(response.getMutualFriendCount() == 1
                    ? "You have 1 friend in common"
                    : "You have " + response.getMutualFriendCount() + " friends in common");
        }
        if (response.getSharedEventCount() > 0) {
            reasons.add(response.getSharedEventCount() == 1
                    ? "1 shared event"
                    : response.getSharedEventCount() + " shared events");
        }
        if (response.getAccessibilityMatchCount() > 0) {
            reasons.add("Similar accessibility preferences");
        }
        if (response.getSharedSavedEventCount() > 0) {
            reasons.add(response.getSharedSavedEventCount() == 1
                    ? "Both saved 1 event"
                    : "Both saved " + response.getSharedSavedEventCount() + " events");
        }
        if (response.getSharedReviewCount() > 0) {
            reasons.add("Similar review activity");
        }
        if (reasons.isEmpty()) {
            reasons.add("Similar community profile");
        }
        return reasons.stream().limit(3).toList();
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

    private int normalizeRecommendationLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_RECOMMENDATION_LIMIT;
        }
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, MAX_RECOMMENDATION_LIMIT);
    }

    private String normalizeSortName(PublicUserResponse user) {
        if (user == null || user.getFullName() == null) {
            return "";
        }
        return user.getFullName().trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
