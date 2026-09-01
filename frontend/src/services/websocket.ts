import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuthStore } from '@/stores/useAuthStore';
import { useWebSocketStore } from '@/stores/useWebSocketStore';

type MessageCallback = (message: IMessage) => void;

class WebSocketService {
  private client: Client | null = null;
  private subscriptions: Map<string, StompSubscription> = new Map();
  private listeners: Map<string, Set<MessageCallback>> = new Map();

  connect(): void {
    const token = useAuthStore.getState().accessToken;
    if (!token) {
      console.warn('WebSocket connect skipped: No access token available');
      return;
    }

    if (this.client?.active) {
      return;
    }

    useWebSocketStore.getState().setConnecting(true);

    this.client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        useWebSocketStore.getState().setConnected(true);

        // Re-subscribe all active destinations
        for (const [destination] of this.listeners.entries()) {
          this.ensureStompSubscription(destination);
        }
      },
      onDisconnect: () => {
        useWebSocketStore.getState().setConnected(false);
      },
      onStompError: (frame) => {
        console.error('WebSocket STOMP error:', frame);
        useWebSocketStore.getState().setConnected(false);
      },
      onWebSocketClose: () => {
        useWebSocketStore.getState().setConnected(false);
      },
    });

    this.client.activate();
  }

  disconnect(): void {
    if (this.client) {
      this.client.deactivate();
      this.client = null;
      this.subscriptions.clear();
      this.listeners.clear();
      useWebSocketStore.getState().setConnected(false);
    }
  }

  subscribe(destination: string, callback: MessageCallback): () => void {
    if (!this.listeners.has(destination)) {
      this.listeners.set(destination, new Set());
    }

    this.listeners.get(destination)!.add(callback);

    if (this.client?.connected) {
      this.ensureStompSubscription(destination);
    }

    // Return unsubscribe cleanup function
    return () => {
      const callbacks = this.listeners.get(destination);
      if (callbacks) {
        callbacks.delete(callback);
        if (callbacks.size === 0) {
          this.listeners.delete(destination);
          const sub = this.subscriptions.get(destination);
          if (sub) {
            sub.unsubscribe();
            this.subscriptions.delete(destination);
          }
        }
      }
    };
  }

  private ensureStompSubscription(destination: string): void {
    if (!this.client?.connected || this.subscriptions.has(destination)) {
      return;
    }

    const sub = this.client.subscribe(destination, (message: IMessage) => {
      const callbacks = this.listeners.get(destination);
      if (callbacks) {
        callbacks.forEach((cb) => {
          try {
            cb(message);
          } catch (err) {
            console.error('Error handling WebSocket message callback:', err);
          }
        });
      }
    });

    this.subscriptions.set(destination, sub);
  }

  send(destination: string, body: object): void {
    if (!this.client || !this.client.connected) {
      console.warn('WebSocket cannot send: not connected');
      return;
    }

    this.client.publish({
      destination,
      body: JSON.stringify(body),
    });
  }

  sendTyping(conversationId: string, typing: boolean): void {
    this.send('/app/chat.typing', {
      conversationId,
      typing,
    });
  }

  isConnected(): boolean {
    return this.client?.connected ?? false;
  }
}

export const websocketService = new WebSocketService();
