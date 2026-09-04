import React, { useEffect, useRef, useMemo } from 'react';
import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { messageService } from '@/services/messageService';
import { useAuthStore } from '@/stores/useAuthStore';
import { MessageItem } from './MessageItem';
import { Loader2, MessageSquare } from 'lucide-react';
import type { Message } from '@/types';

interface MessageListProps {
  conversationId: string;
  onReply: (message: Message) => void;
}

export const MessageList: React.FC<MessageListProps> = ({
  conversationId,
  onReply,
}) => {
  const queryClient = useQueryClient();
  const { user } = useAuthStore();
  const scrollContainerRef = useRef<HTMLDivElement>(null);
  const bottomRef = useRef<HTMLDivElement>(null);

  // Fetch paginated messages with cursor
  const {
    data,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isLoading,
  } = useInfiniteQuery({
    queryKey: ['messages', conversationId],
    queryFn: ({ pageParam }) =>
      messageService.getMessages(conversationId, pageParam as string | undefined, 30),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) =>
      lastPage.hasMore ? lastPage.nextCursor : undefined,
    refetchInterval: 6000,
  });

  // Edit Message Mutation
  const editMutation = useMutation({
    mutationFn: ({
      messageId,
      content,
    }: {
      messageId: string;
      content: string;
    }) => messageService.updateMessage(messageId, { content }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['messages', conversationId] });
      queryClient.invalidateQueries({ queryKey: ['conversations'] });
    },
  });

  // Delete Message Mutation
  const deleteMutation = useMutation({
    mutationFn: (messageId: string) => messageService.deleteMessage(messageId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['messages', conversationId] });
      queryClient.invalidateQueries({ queryKey: ['conversations'] });
    },
  });

  // Flatten and order messages chronologically (oldest first)
  const messages = useMemo(() => {
    if (!data) return [];
    // Pages contain messages ordered descending (newest first from backend cursor pagination)
    const all = data.pages.flatMap((page) => page.content);
    return [...all].reverse();
  }, [data]);

  // Scroll to bottom on initial message load or new message
  useEffect(() => {
    if (bottomRef.current) {
      bottomRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages.length, conversationId]);

  // Automatically mark as read when conversation is loaded
  useEffect(() => {
    if (conversationId && messages.length > 0) {
      messageService.markAsRead(conversationId).catch(() => {});
    }
  }, [conversationId, messages.length]);

  const handleEdit = async (messageId: string, newContent: string) => {
    await editMutation.mutateAsync({ messageId, content: newContent });
  };

  const handleDelete = async (messageId: string) => {
    await deleteMutation.mutateAsync(messageId);
  };

  if (isLoading) {
    return (
      <div className="flex flex-1 items-center justify-center text-slate-500">
        <Loader2 className="h-6 w-6 animate-spin text-indigo-500" />
      </div>
    );
  }

  return (
    <div
      ref={scrollContainerRef}
      className="flex flex-1 flex-col overflow-y-auto p-2 md:p-4"
    >
      {/* Load More Button at Top */}
      {hasNextPage && (
        <div className="flex justify-center py-2">
          <button
            onClick={() => fetchNextPage()}
            disabled={isFetchingNextPage}
            className="flex items-center gap-1.5 rounded-full border border-slate-300 bg-white/90 px-3 py-1 text-xs text-slate-700 shadow-xs transition hover:bg-slate-100 disabled:opacity-50 dark:border-slate-700 dark:bg-slate-800/80 dark:text-slate-300 dark:hover:bg-slate-700"
          >
            {isFetchingNextPage ? (
              <>
                <Loader2 className="h-3.5 w-3.5 animate-spin" />
                <span>Loading older messages...</span>
              </>
            ) : (
              <span>Load older messages</span>
            )}
          </button>
        </div>
      )}

      {/* Empty messages */}
      {messages.length === 0 ? (
        <div className="flex flex-1 flex-col items-center justify-center p-6 text-center text-slate-400 dark:text-slate-500">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-200/80 text-slate-500 mb-2 dark:bg-slate-800/60 dark:text-slate-400">
            <MessageSquare className="h-6 w-6" />
          </div>
          <p className="text-sm font-semibold text-slate-800 dark:text-slate-300">No messages yet</p>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Say hello to start the conversation!
          </p>
        </div>
      ) : (
        <div className="space-y-1">
          {messages.map((message) => (
            <MessageItem
              key={message.id}
              message={message}
              currentUser={user}
              onReply={onReply}
              onEdit={handleEdit}
              onDelete={handleDelete}
            />
          ))}
          <div ref={bottomRef} />
        </div>
      )}
    </div>
  );
};
