package com.chatweb.message.controller;

import com.chatweb.auth.security.UserPrincipal;
import com.chatweb.common.dto.ApiResponse;
import com.chatweb.common.dto.CursorPageResponse;
import com.chatweb.common.exception.ResourceNotFoundException;
import com.chatweb.message.dto.MessageResponse;
import com.chatweb.message.dto.SendMessageRequest;
import com.chatweb.message.dto.UpdateMessageRequest;
import com.chatweb.message.service.MessageService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Messages", description = "Endpoints for sending, retrieving, updating, and deleting chat messages")
@SecurityRequirement(name = "Bearer Authentication")
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/api/conversations/{conversationId}/messages")
    @Operation(summary = "Send a message", description = "Sends a new message in the specified conversation.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Message sent successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error in request payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Bearer token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User is not a member of this conversation"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Conversation or reply message not found")
    })
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Conversation UUID", required = true)
            @PathVariable("conversationId") UUID conversationId,
            @Valid @RequestBody SendMessageRequest request
    ) {
        validatePrincipal(principal);
        MessageResponse response = messageService.sendMessage(principal.getId(), conversationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Message sent successfully", response));
    }

    @GetMapping("/api/conversations/{conversationId}/messages")
    @Operation(summary = "Get message history", description = "Retrieves message history for a conversation using cursor pagination.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Messages retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Bearer token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User is not a member of this conversation")
    })
    public ResponseEntity<ApiResponse<CursorPageResponse<MessageResponse>>> getMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Conversation UUID", required = true)
            @PathVariable("conversationId") UUID conversationId,
            @Parameter(description = "Cursor timestamp (ISO-8601 or epoch millis) for pagination", example = "2026-09-01T12:00:00Z")
            @RequestParam(name = "cursor", required = false) String cursor,
            @Parameter(description = "Number of messages to retrieve (max 50)", example = "20")
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        validatePrincipal(principal);
        CursorPageResponse<MessageResponse> response = messageService.getMessages(principal.getId(), conversationId, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/api/messages/{messageId}")
    @Operation(summary = "Edit a message", description = "Updates the content of a message sent by the authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Message updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or attempting to edit deleted message"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Bearer token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not the author of this message"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Message not found")
    })
    public ResponseEntity<ApiResponse<MessageResponse>> updateMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Message UUID", required = true)
            @PathVariable("messageId") UUID messageId,
            @Valid @RequestBody UpdateMessageRequest request
    ) {
        validatePrincipal(principal);
        MessageResponse response = messageService.updateMessage(principal.getId(), messageId, request);
        return ResponseEntity.ok(ApiResponse.success("Message updated successfully", response));
    }

    @DeleteMapping("/api/messages/{messageId}")
    @Operation(summary = "Delete a message", description = "Soft deletes a message sent by the authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Message deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Bearer token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not the author of this message"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Message not found")
    })
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Message UUID", required = true)
            @PathVariable("messageId") UUID messageId
    ) {
        validatePrincipal(principal);
        messageService.deleteMessage(principal.getId(), messageId);
        return ResponseEntity.ok(ApiResponse.success("Message deleted successfully", null));
    }

    @PostMapping("/api/conversations/{conversationId}/read")
    @Operation(summary = "Mark messages as read", description = "Marks messages in a conversation as read up to a specified message or the latest message.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Conversation marked as read successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid message ID or message does not belong to conversation"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Bearer token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not a member of this conversation")
    })
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Conversation UUID", required = true)
            @PathVariable("conversationId") UUID conversationId,
            @RequestBody(required = false) com.chatweb.message.dto.MarkAsReadRequest request
    ) {
        validatePrincipal(principal);
        messageService.markAsRead(principal.getId(), conversationId, request);
        return ResponseEntity.ok(ApiResponse.success("Conversation marked as read", null));
    }

    private void validatePrincipal(UserPrincipal principal) {
        if (principal == null) {
            throw new ResourceNotFoundException("User not authenticated");
        }
    }
}
