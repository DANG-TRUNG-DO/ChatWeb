import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { userService } from '@/services/userService';
import { useAuthStore } from '@/stores/useAuthStore';
import type { User } from '@/types';

export const useSearchUsers = (query: string, page = 0, size = 20) => {
  return useQuery({
    queryKey: ['users', 'search', query, page, size],
    queryFn: () => userService.searchUsers(query.trim(), page, size),
    enabled: query.trim().length >= 1,
    staleTime: 1000 * 60, // 1 minute
  });
};

export const useUserProfile = (userId?: string | null) => {
  return useQuery({
    queryKey: ['users', userId],
    queryFn: () => (userId ? userService.getUserById(userId) : null),
    enabled: !!userId,
  });
};

export const useUpdateProfile = () => {
  const queryClient = useQueryClient();
  const setUser = useAuthStore((state) => state.setUser);

  return useMutation({
    mutationFn: (data: { displayName?: string; avatarUrl?: string }) =>
      userService.updateProfile(data),
    onSuccess: (updatedUser: User) => {
      setUser(updatedUser);
      queryClient.setQueryData(['currentUser'], updatedUser);
      queryClient.invalidateQueries({ queryKey: ['conversations'] });
    },
  });
};
