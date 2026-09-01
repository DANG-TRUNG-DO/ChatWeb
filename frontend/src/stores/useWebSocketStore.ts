import { create } from 'zustand';

interface TypingState {
  userId: string;
  username: string;
  typing: boolean;
  timestamp: number;
}

interface WebSocketState {
  isConnected: boolean;
  isConnecting: boolean;
  typingUsers: Record<string, TypingState[]>; // conversationId -> TypingState[]
  setConnected: (connected: boolean) => void;
  setConnecting: (connecting: boolean) => void;
  setTyping: (conversationId: string, typingData: { userId: string; username: string; typing: boolean }) => void;
}

export const useWebSocketStore = create<WebSocketState>((set) => ({
  isConnected: false,
  isConnecting: false,
  typingUsers: {},

  setConnected: (isConnected) => set({ isConnected, isConnecting: false }),
  setConnecting: (isConnecting) => set({ isConnecting }),

  setTyping: (conversationId, { userId, username, typing }) =>
    set((state) => {
      const currentList = state.typingUsers[conversationId] || [];
      if (!typing) {
        return {
          typingUsers: {
            ...state.typingUsers,
            [conversationId]: currentList.filter((u) => u.userId !== userId),
          },
        };
      }

      // Add or update typing user
      const updated = [
        ...currentList.filter((u) => u.userId !== userId),
        { userId, username, typing: true, timestamp: Date.now() },
      ];

      return {
        typingUsers: {
          ...state.typingUsers,
          [conversationId]: updated,
        },
      };
    }),
}));
