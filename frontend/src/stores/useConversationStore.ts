import { create } from 'zustand';
import type { Conversation, Message } from '@/types';

interface ConversationState {
  conversations: Conversation[];
  activeConversationId: string | null;
  searchQuery: string;
  isMobileSidebarOpen: boolean;
  isLoadingConversations: boolean;

  setConversations: (conversations: Conversation[]) => void;
  setActiveConversationId: (id: string | null) => void;
  setSearchQuery: (query: string) => void;
  setMobileSidebarOpen: (open: boolean) => void;
  toggleMobileSidebar: () => void;
  setLoadingConversations: (loading: boolean) => void;
  updateConversationLastMessage: (
    conversationId: string,
    message: Message,
    shouldIncrementUnread?: boolean
  ) => void;
  resetUnreadCount: (conversationId: string) => void;
  removeConversation: (conversationId: string) => void;
  addOrUpdateConversation: (conversation: Conversation) => void;
}

export const useConversationStore = create<ConversationState>((set) => ({
  conversations: [],
  activeConversationId: null,
  searchQuery: '',
  isMobileSidebarOpen: false,
  isLoadingConversations: false,

  setConversations: (conversations) => set({ conversations }),
  setActiveConversationId: (id) =>
    set({ activeConversationId: id, isMobileSidebarOpen: false }),
  setSearchQuery: (searchQuery) => set({ searchQuery }),
  setMobileSidebarOpen: (isMobileSidebarOpen) => set({ isMobileSidebarOpen }),
  toggleMobileSidebar: () =>
    set((state) => ({ isMobileSidebarOpen: !state.isMobileSidebarOpen })),
  setLoadingConversations: (isLoadingConversations) =>
    set({ isLoadingConversations }),

  updateConversationLastMessage: (conversationId, message, shouldIncrementUnread = false) =>
    set((state) => ({
      conversations: state.conversations.map((c) =>
        c.id === conversationId
          ? {
              ...c,
              lastMessage: message,
              unreadCount: shouldIncrementUnread ? c.unreadCount + 1 : c.unreadCount,
              updatedAt: message.createdAt,
            }
          : c
      ),
    })),

  resetUnreadCount: (conversationId) =>
    set((state) => ({
      conversations: state.conversations.map((c) =>
        c.id === conversationId ? { ...c, unreadCount: 0 } : c
      ),
    })),

  removeConversation: (conversationId) =>
    set((state) => ({
      conversations: state.conversations.filter((c) => c.id !== conversationId),
      activeConversationId:
        state.activeConversationId === conversationId
          ? null
          : state.activeConversationId,
    })),

  addOrUpdateConversation: (conversation) =>
    set((state) => {
      const exists = state.conversations.some((c) => c.id === conversation.id);
      if (exists) {
        return {
          conversations: state.conversations.map((c) =>
            c.id === conversation.id ? conversation : c
          ),
        };
      }
      return {
        conversations: [conversation, ...state.conversations],
      };
    }),
}));
