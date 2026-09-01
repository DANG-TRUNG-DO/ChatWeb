import { useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { websocketService } from '@/services/websocket';
import { useAuthStore } from '@/stores/useAuthStore';
import { useConversationStore } from '@/stores/useConversationStore';
import { useWebSocketStore } from '@/stores/useWebSocketStore';
import type { Conversation, CursorPageResponse, Message, WebSocketEvent } from '@/types';

export const useWebSocket = () => {
  const queryClient = useQueryClient();
  const { isAuthenticated, accessToken, user } = useAuthStore();
  const { conversations, activeConversationId } = useConversationStore();
  const { isConnected, isConnecting } = useWebSocketStore();

  // Auto-connect when authenticated
  useEffect(() => {
    if (isAuthenticated && accessToken) {
      websocketService.connect();
    } else {
      websocketService.disconnect();
    }

    return () => {
      // Keep connection alive across sub-renders
    };
  }, [isAuthenticated, accessToken]);

  // Subscribe to all user conversations for realtime events
  useEffect(() => {
    if (!isAuthenticated || !isConnected || conversations.length === 0) {
      return;
    }

    const unsubscribers: (() => void)[] = [];

    conversations.forEach((conv) => {
      const unsub = websocketService.subscribe(
        `/topic/conversations/${conv.id}`,
        (stompMessage) => {
          try {
            const event: WebSocketEvent<unknown> = JSON.parse(stompMessage.body);
            handleRealtimeEvent(event, conv.id);
          } catch (err) {
            console.error('Failed to parse WebSocket event:', err);
          }
        }
      );
      unsubscribers.push(unsub);
    });

    return () => {
      unsubscribers.forEach((unsub) => unsub());
    };
  }, [isAuthenticated, isConnected, conversations]);

  const handleRealtimeEvent = (event: WebSocketEvent<unknown>, conversationId: string) => {
    switch (event.type) {
      case 'MESSAGE_SENT': {
        const newMessage = event.payload as Message;

        // 1. Update Messages query cache for this conversation
        queryClient.setQueryData(
          ['messages', conversationId],
          (oldData: { pages: CursorPageResponse<Message>[]; pageParams: unknown[] } | undefined) => {
            if (!oldData) return oldData;

            // Check if message already in cache to prevent duplicate
            const alreadyExists = oldData.pages.some((page) =>
              page.content.some((m) => m.id === newMessage.id)
            );
            if (alreadyExists) return oldData;

            const firstPage = oldData.pages[0];
            const updatedFirstPage = {
              ...firstPage,
              content: [newMessage, ...firstPage.content],
            };

            return {
              ...oldData,
              pages: [updatedFirstPage, ...oldData.pages.slice(1)],
            };
          }
        );

        // 2. Update Conversations query cache (lastMessage & unreadCount)
        queryClient.setQueryData(
          ['conversations'],
          (oldConversations: Conversation[] | undefined) => {
            if (!oldConversations) return oldConversations;

            return oldConversations.map((c) => {
              if (c.id === conversationId) {
                const isCurrentActive = activeConversationId === conversationId;
                const isOwnMessage = (newMessage.senderId || newMessage.sender?.id) === user?.id;
                const shouldIncrementUnread = !isCurrentActive && !isOwnMessage;

                return {
                  ...c,
                  lastMessage: newMessage,
                  unreadCount: shouldIncrementUnread
                    ? c.unreadCount + 1
                    : c.unreadCount,
                  updatedAt: newMessage.createdAt,
                };
              }
              return c;
            });
          }
        );
        break;
      }

      case 'MESSAGE_UPDATED': {
        const updatedMessage = event.payload as Message;

        queryClient.setQueryData(
          ['messages', conversationId],
          (oldData: { pages: CursorPageResponse<Message>[]; pageParams: unknown[] } | undefined) => {
            if (!oldData) return oldData;

            return {
              ...oldData,
              pages: oldData.pages.map((page) => ({
                ...page,
                content: page.content.map((m) =>
                  m.id === updatedMessage.id ? updatedMessage : m
                ),
              })),
            };
          }
        );

        queryClient.setQueryData(
          ['conversations'],
          (oldConversations: Conversation[] | undefined) => {
            if (!oldConversations) return oldConversations;

            return oldConversations.map((c) => {
              if (c.id === conversationId && c.lastMessage?.id === updatedMessage.id) {
                return {
                  ...c,
                  lastMessage: updatedMessage,
                };
              }
              return c;
            });
          }
        );
        break;
      }

      case 'MESSAGE_DELETED': {
        const payload = event.payload as { messageId: string };
        const messageId = payload.messageId;

        queryClient.setQueryData(
          ['messages', conversationId],
          (oldData: { pages: CursorPageResponse<Message>[]; pageParams: unknown[] } | undefined) => {
            if (!oldData) return oldData;

            return {
              ...oldData,
              pages: oldData.pages.map((page) => ({
                ...page,
                content: page.content.map((m) =>
                  m.id === messageId ? { ...m, deleted: true, content: null } : m
                ),
              })),
            };
          }
        );

        queryClient.setQueryData(
          ['conversations'],
          (oldConversations: Conversation[] | undefined) => {
            if (!oldConversations) return oldConversations;

            return oldConversations.map((c) => {
              if (c.id === conversationId && c.lastMessage?.id === messageId) {
                return {
                  ...c,
                  lastMessage: { ...c.lastMessage, deleted: true, content: null },
                };
              }
              return c;
            });
          }
        );
        break;
      }

      case 'MESSAGE_READ': {
        const payload = event.payload as { conversationId: string; userId: string };
        if (payload.userId === user?.id) {
          queryClient.setQueryData(
            ['conversations'],
            (oldConversations: Conversation[] | undefined) => {
              if (!oldConversations) return oldConversations;

              return oldConversations.map((c) => {
                if (c.id === conversationId) {
                  return { ...c, unreadCount: 0 };
                }
                return c;
              });
            }
          );
        }
        break;
      }

      case 'TYPING': {
        const payload = event.payload as {
          conversationId: string;
          userId: string;
          username: string;
          typing: boolean;
        };
        if (payload.userId !== user?.id) {
          useWebSocketStore.getState().setTyping(conversationId, payload);
        }
        break;
      }

      default:
        break;
    }
  };

  return {
    isConnected,
    isConnecting,
    sendTyping: (convId: string, typing: boolean) =>
      websocketService.sendTyping(convId, typing),
  };
};
