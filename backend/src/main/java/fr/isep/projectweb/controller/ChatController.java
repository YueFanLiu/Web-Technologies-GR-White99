package fr.isep.projectweb.controller;

import fr.isep.projectweb.model.dto.request.ChatMessageRequest;
import fr.isep.projectweb.model.dto.request.ChatReadRequest;
import fr.isep.projectweb.model.dto.request.DirectChatRequest;
import fr.isep.projectweb.model.dto.response.ChatConversationResponse;
import fr.isep.projectweb.model.dto.response.ChatMessageResponse;
import fr.isep.projectweb.model.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chats")
@Tag(name = "Chats", description = "Direct chat endpoints")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    @Operation(summary = "Get the current authenticated user's chat conversations")
    public List<ChatConversationResponse> getMyConversations(@AuthenticationPrincipal Jwt jwt) {
        return chatService.getMyConversations(jwt);
    }

    @PostMapping("/direct")
    @Operation(summary = "Get or create a direct chat with a friend")
    public ChatConversationResponse getOrCreateDirectConversation(@RequestBody DirectChatRequest request,
                                                                  @AuthenticationPrincipal Jwt jwt) {
        return chatService.getOrCreateDirectConversation(request, jwt);
    }

    @GetMapping("/{conversationId}/messages")
    @Operation(summary = "Get messages in a chat conversation")
    public List<ChatMessageResponse> getMessages(@PathVariable UUID conversationId,
                                                 @RequestParam(required = false) LocalDateTime before,
                                                 @RequestParam(required = false) Integer limit,
                                                 @AuthenticationPrincipal Jwt jwt) {
        return chatService.getMessages(conversationId, before, limit, jwt);
    }

    @PostMapping("/{conversationId}/messages")
    @Operation(summary = "Send a message to a chat conversation")
    public ChatMessageResponse sendMessage(@PathVariable UUID conversationId,
                                           @RequestBody ChatMessageRequest request,
                                           @AuthenticationPrincipal Jwt jwt) {
        return chatService.sendMessage(conversationId, request, jwt);
    }

    @PostMapping("/{conversationId}/read")
    @Operation(summary = "Mark a chat conversation as read")
    public ChatConversationResponse markAsRead(@PathVariable UUID conversationId,
                                               @RequestBody(required = false) ChatReadRequest request,
                                               @AuthenticationPrincipal Jwt jwt) {
        return chatService.markAsRead(conversationId, request, jwt);
    }
}
