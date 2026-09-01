import React from 'react';
import { MainLayout } from '@/components/layout/MainLayout';
import { useConversationStore } from '@/stores/useConversationStore';

export const ChatPage: React.FC = () => {
  const { conversations, activeConversationId } = useConversationStore();

  const activeConversation = conversations.find((c) => c.id === activeConversationId) || null;

  return (
    <MainLayout activeConversation={activeConversation}>
      <div className="flex h-full items-center justify-center p-6 text-slate-400">
        <p className="text-xs">Select messages to start chat...</p>
      </div>
    </MainLayout>
  );
};
