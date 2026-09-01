import React, { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { messageService } from '@/services/messageService';
import { useWebSocket } from '@/hooks/useWebSocket';
import { MessageList } from './MessageList';
import { MessageInput } from './MessageInput';
import type { Message } from '@/types';

interface ChatAreaProps {
  conversationId: string;
}

export const ChatArea: React.FC<ChatAreaProps> = ({ conversationId }) => {
  const queryClient = useQueryClient();
  const { sendTyping } = useWebSocket();
  const [replyingTo, setReplyingTo] = useState<Message | null>(null);

  // Send Message Mutation
  const sendMutation = useMutation({
    mutationFn: ({
      content,
      replyToId,
    }: {
      content: string;
      replyToId?: string;
    }) =>
      messageService.sendMessage(conversationId, {
        content,
        type: 'TEXT',
        replyToId,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['messages', conversationId] });
      queryClient.invalidateQueries({ queryKey: ['conversations'] });
    },
  });

  const handleSendMessage = async (content: string, replyToId?: string) => {
    await sendMutation.mutateAsync({ content, replyToId });
  };

  return (
    <div className="flex h-full flex-1 flex-col overflow-hidden bg-slate-950">
      {/* Scrollable Message List */}
      <MessageList
        conversationId={conversationId}
        onReply={(msg) => setReplyingTo(msg)}
      />

      {/* Message Input Bottom Bar */}
      <MessageInput
        onSendMessage={handleSendMessage}
        replyingTo={replyingTo}
        onCancelReply={() => setReplyingTo(null)}
        onTyping={(typing) => sendTyping(conversationId, typing)}
        disabled={sendMutation.isPending}
      />
    </div>
  );
};
