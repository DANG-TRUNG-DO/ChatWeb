package com.chatweb.realtime.dto;

public enum WebSocketEventType {
    MESSAGE_SENT,
    MESSAGE_UPDATED,
    MESSAGE_DELETED,
    MESSAGE_READ,
    TYPING,
    USER_ONLINE,
    USER_OFFLINE
}
