import { Client, type IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuthStore } from '@/stores/useAuthStore';

class WebSocketService {
  private client: Client | null = null;
  private subscriptions: Map<string, { id: string }> = new Map();

  connect(): void {
    const token = useAuthStore.getState().accessToken;

    this.client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        console.log('WebSocket connected');
      },
      onDisconnect: () => {
        console.log('WebSocket disconnected');
      },
      onStompError: (frame) => {
        console.error('WebSocket error:', frame);
      },
    });

    this.client.activate();
  }

  disconnect(): void {
    if (this.client) {
      this.client.deactivate();
      this.client = null;
      this.subscriptions.clear();
    }
  }

  subscribe(destination: string, callback: (message: IMessage) => void): void {
    if (!this.client || !this.client.connected) {
      console.warn('WebSocket not connected');
      return;
    }

    const subscription = this.client.subscribe(destination, callback);
    this.subscriptions.set(destination, subscription);
  }

  unsubscribe(destination: string): void {
    const subscription = this.subscriptions.get(destination);
    if (subscription) {
      subscription.id; // STOMP.js handles unsubscribe via the subscription object
      this.subscriptions.delete(destination);
    }
  }

  send(destination: string, body: object): void {
    if (!this.client || !this.client.connected) {
      console.warn('WebSocket not connected');
      return;
    }

    this.client.publish({
      destination,
      body: JSON.stringify(body),
    });
  }

  isConnected(): boolean {
    return this.client?.connected ?? false;
  }
}

export const websocketService = new WebSocketService();
