import api from './api';
import type {
  ApiResponse,
  CursorPageResponse,
  MarkAsReadRequest,
  Message,
  SendMessageRequest,
  UpdateMessageRequest,
} from '@/types';

export const messageService = {
  async getMessages(
    conversationId: string,
    cursor?: string,
    limit = 30
  ): Promise<CursorPageResponse<Message>> {
    const response = await api.get<ApiResponse<CursorPageResponse<Message>>>(
      `/conversations/${conversationId}/messages`,
      {
        params: { cursor, limit },
      }
    );
    return response.data.data;
  },

  async sendMessage(
    conversationId: string,
    data: SendMessageRequest
  ): Promise<Message> {
    const response = await api.post<ApiResponse<Message>>(
      `/conversations/${conversationId}/messages`,
      data
    );
    return response.data.data;
  },

  async updateMessage(
    messageId: string,
    data: UpdateMessageRequest
  ): Promise<Message> {
    const response = await api.put<ApiResponse<Message>>(
      `/messages/${messageId}`,
      data
    );
    return response.data.data;
  },

  async deleteMessage(messageId: string): Promise<void> {
    await api.delete<ApiResponse<void>>(`/messages/${messageId}`);
  },

  async markAsRead(
    conversationId: string,
    data?: MarkAsReadRequest
  ): Promise<void> {
    await api.post<ApiResponse<void>>(
      `/conversations/${conversationId}/read`,
      data
    );
  },
};
