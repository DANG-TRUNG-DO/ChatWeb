import api from './api';
import type { ApiResponse, Conversation, CreateDirectConversationRequest } from '@/types';

export const conversationService = {
  async getConversations(): Promise<Conversation[]> {
    const response = await api.get<ApiResponse<Conversation[]>>('/conversations');
    return response.data.data;
  },

  async getConversationById(id: string): Promise<Conversation> {
    const response = await api.get<ApiResponse<Conversation>>(`/conversations/${id}`);
    return response.data.data;
  },

  async createDirectConversation(recipientId: string): Promise<Conversation> {
    const request: CreateDirectConversationRequest = { recipientId };
    const response = await api.post<ApiResponse<Conversation>>('/conversations', request);
    return response.data.data;
  },

  async deleteConversation(id: string): Promise<void> {
    await api.delete<ApiResponse<void>>(`/conversations/${id}`);
  },
};
