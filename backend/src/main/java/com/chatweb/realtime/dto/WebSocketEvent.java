package com.chatweb.realtime.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketEvent<T> {

    private WebSocketEventType type;
    private T payload;
    private Instant timestamp;

    public static <T> WebSocketEvent<T> of(WebSocketEventType type, T payload) {
        return WebSocketEvent.<T>builder()
                .type(type)
                .payload(payload)
                .timestamp(Instant.now())
                .build();
    }
}
