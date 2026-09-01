import React, { useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { conversationService } from '@/services/conversationService';
import { userService } from '@/services/userService';
import { useConversationStore } from '@/stores/useConversationStore';
import { useAuthStore } from '@/stores/useAuthStore';
import { ConversationItem } from './ConversationItem';
import { UserSearchResultItem } from './UserSearchResultItem';
import { Loader2, MessageSquare, Search, UserCheck } from 'lucide-react';
import type { Conversation, User } from '@/types';

export const ConversationList: React.FC = () => {
  const queryClient = useQueryClient();
  const { user: currentUser } = useAuthStore();
  const {
    activeConversationId,
    setActiveConversationId,
    searchQuery,
    setSearchQuery,
    setConversations,
    addOrUpdateConversation,
  } = useConversationStore();

  // Fetch Conversations
  const {
    data: conversations = [],
    isLoading: isLoadingConversations,
  } = useQuery({
    queryKey: ['conversations'],
    queryFn: async () => {
      const data = await conversationService.getConversations();
      setConversations(data);
      return data;
    },
    refetchInterval: 10000,
  });

  // Search Users query (when searchQuery is present)
  const {
    data: searchUserResult,
    isLoading: isSearchingUsers,
  } = useQuery({
    queryKey: ['users', 'search', searchQuery],
    queryFn: () => userService.searchUsers(searchQuery.trim(), 0, 10),
    enabled: searchQuery.trim().length >= 1,
  });

  // Mutation to create or get direct conversation
  const createConversationMutation = useMutation({
    mutationFn: (recipientId: string) =>
      conversationService.createDirectConversation(recipientId),
    onSuccess: (newConv: Conversation) => {
      addOrUpdateConversation(newConv);
      queryClient.invalidateQueries({ queryKey: ['conversations'] });
      setActiveConversationId(newConv.id);
      setSearchQuery('');
    },
  });

  // Sorted and filtered conversations
  const sortedConversations = useMemo(() => {
    return [...conversations].sort((a, b) => {
      const timeA = new Date(a.lastMessage?.createdAt || a.updatedAt || a.createdAt).getTime();
      const timeB = new Date(b.lastMessage?.createdAt || b.updatedAt || b.createdAt).getTime();
      return timeB - timeA;
    });
  }, [conversations]);

  const filteredConversations = useMemo(() => {
    if (!searchQuery.trim()) return sortedConversations;
    const q = searchQuery.toLowerCase().trim();

    return sortedConversations.filter((c) => {
      if (c.type === 'DIRECT') {
        const partner = c.partner;
        const other = c.members?.find((m) => m.userId !== currentUser?.id);
        const nameMatch = (partner?.displayName || other?.user?.displayName || c.name)?.toLowerCase().includes(q);
        const usernameMatch = (partner?.username || other?.user?.username)?.toLowerCase().includes(q);
        return nameMatch || usernameMatch;
      }
      return c.name?.toLowerCase().includes(q);
    });
  }, [sortedConversations, searchQuery, currentUser]);

  const searchedUsers = searchUserResult?.content || [];

  if (isLoadingConversations && conversations.length === 0) {
    return (
      <div className="flex h-48 flex-col items-center justify-center gap-3 text-slate-500">
        <Loader2 className="h-6 w-6 animate-spin text-indigo-500" />
        <p className="text-xs">Loading conversations...</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* If search query is active, show Global User Search results */}
      {searchQuery.trim().length >= 1 && (
        <div className="space-y-2">
          <div className="flex items-center gap-1.5 px-2 text-[11px] font-semibold uppercase tracking-wider text-slate-400">
            <UserCheck className="h-3.5 w-3.5 text-indigo-400" />
            <span>People</span>
          </div>

          {isSearchingUsers ? (
            <div className="flex items-center justify-center py-4 text-xs text-slate-500">
              <Loader2 className="h-4 w-4 animate-spin mr-2 text-indigo-400" />
              Searching users...
            </div>
          ) : searchedUsers.length > 0 ? (
            <div className="space-y-1">
              {searchedUsers.map((user: User) => (
                <UserSearchResultItem
                  key={user.id}
                  user={user}
                  isLoading={createConversationMutation.isPending}
                  onClick={() => createConversationMutation.mutate(user.id)}
                />
              ))}
            </div>
          ) : (
            <p className="px-3 py-2 text-xs text-slate-500">No users found</p>
          )}

          <div className="border-t border-slate-800/80 my-3" />
        </div>
      )}

      {/* Conversations Section */}
      <div className="space-y-1">
        {searchQuery.trim().length >= 1 && (
          <div className="flex items-center gap-1.5 px-2 text-[11px] font-semibold uppercase tracking-wider text-slate-400 mb-2">
            <MessageSquare className="h-3.5 w-3.5 text-slate-400" />
            <span>Chats ({filteredConversations.length})</span>
          </div>
        )}

        {filteredConversations.length > 0 ? (
          filteredConversations.map((conv) => (
            <ConversationItem
              key={conv.id}
              conversation={conv}
              isActive={activeConversationId === conv.id}
              onClick={() => setActiveConversationId(conv.id)}
            />
          ))
        ) : searchQuery.trim().length >= 1 ? (
          <p className="px-3 py-2 text-xs text-slate-500">No matching conversations</p>
        ) : (
          <div className="flex flex-col items-center justify-center p-8 text-center text-slate-500">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-800/60 text-slate-400 mb-3">
              <Search className="h-5 w-5" />
            </div>
            <p className="text-sm font-semibold text-slate-300">No conversations yet</p>
            <p className="mt-1 text-xs text-slate-500 max-w-[200px]">
              Search for users above to start your first chat.
            </p>
          </div>
        )}
      </div>
    </div>
  );
};
