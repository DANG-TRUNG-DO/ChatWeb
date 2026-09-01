import { create } from 'zustand';
import type { Conversation } from '@/types';

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
}));
