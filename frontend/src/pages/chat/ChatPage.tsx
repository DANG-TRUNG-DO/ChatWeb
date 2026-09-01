import React from 'react';
import { MainLayout } from '@/components/layout/MainLayout';
import { ConversationList } from '@/components/conversation/ConversationList';
import { ChatArea } from '@/components/chat/ChatArea';
import { useConversationStore } from '@/stores/useConversationStore';

export const ChatPage: React.FC = () => {
  const { conversations, activeConversationId } = useConversationStore();

  const activeConversation =
    conversations.find((c) => c.id === activeConversationId) || null;

  return (
    <MainLayout
      sidebarContent={<ConversationList />}
      activeConversation={activeConversation}
    >
      {activeConversationId ? (
        <ChatArea conversationId={activeConversationId} />
      ) : (
        <div className="flex h-full items-center justify-center p-6 text-slate-400">
          <p className="text-xs">Select a conversation to start messaging</p>
        </div>
      )}
    </MainLayout>
  );
};
