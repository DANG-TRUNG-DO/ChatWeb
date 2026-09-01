import { create } from 'zustand';
import type { Message } from '@/types';

interface MessageState {
  replyingTo: Message | null;
  editingMessageId: string | null;
  drafts: Record<string, string>; // conversationId -> text draft

  setReplyingTo: (message: Message | null) => void;
  setEditingMessageId: (messageId: string | null) => void;
  setDraft: (conversationId: string, text: string) => void;
  getDraft: (conversationId: string) => string;
  clearDraft: (conversationId: string) => void;
}

export const useMessageStore = create<MessageState>((set, get) => ({
  replyingTo: null,
  editingMessageId: null,
  drafts: {},

  setReplyingTo: (replyingTo) => set({ replyingTo }),
  setEditingMessageId: (editingMessageId) => set({ editingMessageId }),
  setDraft: (conversationId, text) =>
    set((state) => ({
      drafts: {
        ...state.drafts,
        [conversationId]: text,
      },
    })),
  getDraft: (conversationId) => get().drafts[conversationId] || '',
  clearDraft: (conversationId) =>
    set((state) => {
      const copy = { ...state.drafts };
      delete copy[conversationId];
      return { drafts: copy };
    }),
}));
