import api from './api';
import type { ApiResponse, PageResponse, User } from '@/types';

export const userService = {
  async searchUsers(query: string, page = 0, size = 20): Promise<PageResponse<User>> {
    const response = await api.get<ApiResponse<PageResponse<User>>>('/users/search', {
      params: { q: query, page, size },
    });
    return response.data.data;
  },

  async getUserById(id: string): Promise<User> {
    const response = await api.get<ApiResponse<User>>(`/users/${id}`);
    return response.data.data;
  },

  async updateProfile(data: { displayName?: string; avatarUrl?: string }): Promise<User> {
    const response = await api.put<ApiResponse<User>>('/users/me', data);
    return response.data.data;
  },
};
