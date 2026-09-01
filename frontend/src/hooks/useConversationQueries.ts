import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { conversationService } from '@/services/conversationService';
import { useConversationStore } from '@/stores/useConversationStore';
import type { Conversation } from '@/types';

export const useConversations = () => {
  const setConversations = useConversationStore((state) => state.setConversations);

  return useQuery({
    queryKey: ['conversations'],
    queryFn: async () => {
      const data = await conversationService.getConversations();
      setConversations(data);
      return data;
    },
    staleTime: 1000 * 30, // 30 seconds
  });
};

export const useConversation = (id?: string | null) => {
  return useQuery({
    queryKey: ['conversations', id],
    queryFn: () => (id ? conversationService.getConversationById(id) : null),
    enabled: !!id,
    staleTime: 1000 * 60,
  });
};

export const useCreateDirectConversation = () => {
  const queryClient = useQueryClient();
  const { setActiveConversationId, setSearchQuery, addOrUpdateConversation } =
    useConversationStore();

  return useMutation({
    mutationFn: (recipientId: string) =>
      conversationService.createDirectConversation(recipientId),
    onSuccess: (newConv: Conversation) => {
      addOrUpdateConversation(newConv);
      setActiveConversationId(newConv.id);
      setSearchQuery('');
      queryClient.invalidateQueries({ queryKey: ['conversations'] });
    },
  });
};

export const useDeleteConversation = () => {
  const queryClient = useQueryClient();
  const { removeConversation } = useConversationStore();

  return useMutation({
    mutationFn: (conversationId: string) =>
      conversationService.deleteConversation(conversationId),
    onSuccess: (_, conversationId) => {
      removeConversation(conversationId);
      queryClient.invalidateQueries({ queryKey: ['conversations'] });
      queryClient.removeQueries({ queryKey: ['messages', conversationId] });
    },
  });
};
