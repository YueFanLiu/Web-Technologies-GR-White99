package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.ChatConversationRepository;
import fr.isep.projectweb.model.dao.ChatMessageRepository;
import fr.isep.projectweb.model.dao.ChatParticipantRepository;
import fr.isep.projectweb.model.dao.UserRepository;
import fr.isep.projectweb.model.dto.request.ChatMessageRequest;
import fr.isep.projectweb.model.dto.request.ChatReadRequest;
import fr.isep.projectweb.model.dto.request.DirectChatRequest;
import fr.isep.projectweb.model.dto.response.ChatConversationResponse;
import fr.isep.projectweb.model.dto.response.ChatMessageResponse;
import fr.isep.projectweb.model.entity.ChatConversation;
import fr.isep.projectweb.model.entity.ChatMessage;
import fr.isep.projectweb.model.entity.ChatParticipant;
import fr.isep.projectweb.model.entity.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class ChatService {

    private static final String CONVERSATION_TYPE_DIRECT = "DIRECT";
    private static final String MESSAGE_TYPE_TEXT = "TEXT";
    private static final Set<String> ALLOWED_MESSAGE_TYPES = Set.of("TEXT", "IMAGE", "SYSTEM");
    private static final int DEFAULT_MESSAGE_LIMIT = 30;
    private static final int MAX_MESSAGE_LIMIT = 100;

    private final ChatConversationRepository chatConversationRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final FriendService friendService;
    private final NotificationService notificationService;

    public ChatService(ChatConversationRepository chatConversationRepository,
                       ChatParticipantRepository chatParticipantRepository,
                       ChatMessageRepository chatMessageRepository,
                       UserRepository userRepository,
                       CurrentUserService currentUserService,
                       FriendService friendService,
                       NotificationService notificationService) {
        this.chatConversationRepository = chatConversationRepository;
        this.chatParticipantRepository = chatParticipantRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.friendService = friendService;
        this.notificationService = notificationService;
    }

    public List<ChatConversationResponse> getMyConversations(Jwt jwt) {
        UUID currentUserId = currentUserService.getCurrentUserId(jwt);
        return chatConversationRepository.findByParticipantUserId(currentUserId)
                .stream()
                .map(conversation -> toConversationResponse(conversation, currentUserId))
                .toList();
    }

    public ChatConversationResponse getOrCreateDirectConversation(DirectChatRequest request, Jwt jwt) {
        User currentUser = currentUserService.getOrCreateCurrentUser(jwt);
        UUID otherUserId = request.getUserId();
        if (otherUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User id must not be null");
        }
        if (Objects.equals(currentUser.getId(), otherUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot create a chat with yourself");
        }
        if (!friendService.areFriends(currentUser.getId(), otherUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only chat with friends");
        }

        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ChatConversation conversation = chatConversationRepository
                .findDirectConversation(currentUser.getId(), otherUserId)
                .orElseGet(() -> createDirectConversation(currentUser, otherUser));

        return toConversationResponse(conversation, currentUser.getId());
    }

    public List<ChatMessageResponse> getMessages(UUID conversationId, LocalDateTime before, Integer limit, Jwt jwt) {
        UUID currentUserId = currentUserService.getCurrentUserId(jwt);
        ensureParticipant(conversationId, currentUserId);

        return chatMessageRepository.findPageByConversationId(
                        conversationId,
                        before,
                        PageRequest.of(0, normalizeMessageLimit(limit))
                )
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    public ChatMessageResponse sendMessage(UUID conversationId, ChatMessageRequest request, Jwt jwt) {
        User currentUser = currentUserService.getOrCreateCurrentUser(jwt);
        ChatConversation conversation = findConversation(conversationId);
        ensureParticipant(conversationId, currentUser.getId());

        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setSender(currentUser);
        message.setContent(normalizeContent(request.getContent()));
        message.setMessageType(normalizeMessageType(request.getMessageType()));

        ChatMessage savedMessage = chatMessageRepository.save(message);
        conversation.setUpdatedAt(LocalDateTime.now());
        chatConversationRepository.save(conversation);
        notifyMessageRecipients(savedMessage);
        return toMessageResponse(savedMessage);
    }

    private void notifyMessageRecipients(ChatMessage message) {
        chatParticipantRepository.findByConversationIdOrderByJoinedAtAsc(message.getConversation().getId())
                .stream()
                .map(ChatParticipant::getUser)
                .filter(user -> !Objects.equals(user.getId(), message.getSender().getId()))
                .forEach(user -> notificationService.create(
                        user,
                        message.getSender(),
                        NotificationService.CHAT_MESSAGE_RECEIVED,
                        "New message",
                        message.getSender().getFullName() + " sent you a message",
                        "CHAT_CONVERSATION",
                        message.getConversation().getId(),
                        "CHAT_MESSAGE",
                        message.getId(),
                        "chat_message:" + message.getId(),
                        java.util.Map.of(
                                "conversationId", message.getConversation().getId().toString(),
                                "messageId", message.getId().toString()
                        )
                ));
    }

    public ChatConversationResponse markAsRead(UUID conversationId, ChatReadRequest request, Jwt jwt) {
        UUID currentUserId = currentUserService.getCurrentUserId(jwt);
        ChatConversation conversation = findConversation(conversationId);
        ChatParticipant participant = chatParticipantRepository.findByConversationIdAndUserId(conversationId, currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a participant in this chat"));

        ChatMessage message = resolveReadMessage(conversationId, request != null ? request.getMessageId() : null);
        if (message != null) {
            participant.setLastReadMessage(message);
            chatParticipantRepository.save(participant);
        }

        return toConversationResponse(conversation, currentUserId);
    }

    private ChatConversation createDirectConversation(User firstUser, User secondUser) {
        ChatConversation conversation = new ChatConversation();
        conversation.setType(CONVERSATION_TYPE_DIRECT);
        if (firstUser.getId().compareTo(secondUser.getId()) <= 0) {
            conversation.setDirectUserOne(firstUser);
            conversation.setDirectUserTwo(secondUser);
        } else {
            conversation.setDirectUserOne(secondUser);
            conversation.setDirectUserTwo(firstUser);
        }

        ChatConversation savedConversation = chatConversationRepository.save(conversation);
        createParticipant(savedConversation, firstUser);
        createParticipant(savedConversation, secondUser);
        return savedConversation;
    }

    private void createParticipant(ChatConversation conversation, User user) {
        ChatParticipant participant = new ChatParticipant();
        participant.setConversation(conversation);
        participant.setUser(user);
        chatParticipantRepository.save(participant);
    }

    private ChatConversation findConversation(UUID conversationId) {
        return chatConversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat conversation not found"));
    }

    private void ensureParticipant(UUID conversationId, UUID userId) {
        if (!chatParticipantRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a participant in this chat");
        }
    }

    private ChatMessage resolveReadMessage(UUID conversationId, UUID messageId) {
        if (messageId == null) {
            return chatMessageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversationId).orElse(null);
        }

        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat message not found"));
        if (!Objects.equals(message.getConversation().getId(), conversationId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message does not belong to this conversation");
        }
        return message;
    }

    private ChatConversationResponse toConversationResponse(ChatConversation conversation, UUID currentUserId) {
        ChatConversationResponse response = new ChatConversationResponse();
        response.setId(conversation.getId());
        response.setType(conversation.getType());
        response.setParticipants(chatParticipantRepository.findByConversationIdOrderByJoinedAtAsc(conversation.getId())
                .stream()
                .map(ChatParticipant::getUser)
                .map(ResponseMapper::toPublicUserResponse)
                .toList());
        response.setLastMessage(chatMessageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversation.getId())
                .map(this::toMessageResponse)
                .orElse(null));
        response.setUnreadCount(resolveUnreadCount(conversation.getId(), currentUserId));
        response.setCreatedAt(conversation.getCreatedAt());
        response.setUpdatedAt(conversation.getUpdatedAt());
        return response;
    }

    private long resolveUnreadCount(UUID conversationId, UUID currentUserId) {
        ChatParticipant participant = chatParticipantRepository.findByConversationIdAndUserId(conversationId, currentUserId)
                .orElse(null);
        if (participant == null) {
            return 0;
        }
        if (participant.getLastReadMessage() == null || participant.getLastReadMessage().getCreatedAt() == null) {
            return chatMessageRepository.countByConversationIdAndSenderIdNot(conversationId, currentUserId);
        }
        return chatMessageRepository.countByConversationIdAndSenderIdNotAndCreatedAtAfter(
                conversationId,
                currentUserId,
                participant.getLastReadMessage().getCreatedAt()
        );
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setId(message.getId());
        response.setConversationId(message.getConversation().getId());
        response.setSender(ResponseMapper.toPublicUserResponse(message.getSender()));
        response.setContent(message.getDeletedAt() == null ? message.getContent() : null);
        response.setMessageType(message.getMessageType());
        response.setCreatedAt(message.getCreatedAt());
        response.setEditedAt(message.getEditedAt());
        response.setDeletedAt(message.getDeletedAt());
        return response;
    }

    private int normalizeMessageLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_MESSAGE_LIMIT;
        }
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, MAX_MESSAGE_LIMIT);
    }

    private String normalizeContent(String content) {
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content must not be blank");
        }
        return content.trim();
    }

    private String normalizeMessageType(String messageType) {
        if (messageType == null || messageType.isBlank()) {
            return MESSAGE_TYPE_TEXT;
        }
        String normalized = messageType.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_MESSAGE_TYPES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported message type");
        }
        return normalized;
    }
}
