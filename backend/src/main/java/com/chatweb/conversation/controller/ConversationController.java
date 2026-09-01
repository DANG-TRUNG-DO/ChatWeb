package com.chatweb.conversation.controller;

import com.chatweb.auth.security.UserPrincipal;
import com.chatweb.common.dto.ApiResponse;
import com.chatweb.common.exception.ResourceNotFoundException;
import com.chatweb.conversation.dto.ConversationDetailResponse;
import com.chatweb.conversation.dto.ConversationSummaryResponse;
import com.chatweb.conversation.dto.CreateDirectConversationRequest;
import com.chatweb.conversation.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Tag(name = "Conversations", description = "Endpoints for direct/group chat conversations and memberships")
@SecurityRequirement(name = "Bearer Authentication")
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping
    @Operation(summary = "Create or get direct conversation", description = "Gets an existing direct conversation with the specified user or creates a new one.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Existing direct conversation retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Direct conversation created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid recipient or cannot chat with yourself"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Bearer token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Recipient user not found")
    })
    public ResponseEntity<ApiResponse<ConversationDetailResponse>> createOrGetDirectConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateDirectConversationRequest request
    ) {
        validatePrincipal(principal);
        ConversationDetailResponse response = conversationService.getOrCreateDirectConversation(
                principal.getId(), request.getRecipientId());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Get user conversations", description = "Retrieves all conversations the authenticated user participates in, sorted by latest message/activity.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Conversations retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Bearer token missing or invalid")
    })
    public ResponseEntity<ApiResponse<List<ConversationSummaryResponse>>> getUserConversations(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        validatePrincipal(principal);
        List<ConversationSummaryResponse> response = conversationService.getUserConversations(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get conversation by ID", description = "Retrieves details and member list for a specific conversation.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Conversation details retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Bearer token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not a member of this conversation"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Conversation not found")
    })
    public ResponseEntity<ApiResponse<ConversationDetailResponse>> getConversationById(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Conversation UUID", required = true)
            @PathVariable("id") UUID id
    ) {
        validatePrincipal(principal);
        ConversationDetailResponse response = conversationService.getConversationById(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete or leave conversation", description = "Deletes a direct conversation, or leaves/deletes a group conversation depending on member role.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Conversation deleted or left successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Bearer token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not a member of this conversation"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Conversation not found")
    })
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Conversation UUID", required = true)
            @PathVariable("id") UUID id
    ) {
        validatePrincipal(principal);
        conversationService.deleteConversation(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Conversation deleted successfully", null));
    }

    private void validatePrincipal(UserPrincipal principal) {
        if (principal == null) {
            throw new ResourceNotFoundException("User not authenticated");
        }
    }
}
