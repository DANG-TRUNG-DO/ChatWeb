import React from 'react';
import { Sidebar } from './Sidebar';
import { ChatHeader } from './ChatHeader';
import { EmptyChatState } from './EmptyChatState';
import { useAuthStore } from '@/stores/useAuthStore';
import { useConversationStore } from '@/stores/useConversationStore';
import type { Conversation } from '@/types';

interface MainLayoutProps {
  children?: React.ReactNode;
  sidebarContent?: React.ReactNode;
  activeConversation?: Conversation | null;
  onOpenNewChat?: () => void;
  onOpenInfo?: () => void;
}

export const MainLayout: React.FC<MainLayoutProps> = ({
  children,
  sidebarContent,
  activeConversation,
  onOpenNewChat,
  onOpenInfo,
}) => {
  const { user } = useAuthStore();
  const { activeConversationId } = useConversationStore();

  return (
    <div className="flex h-screen w-full overflow-hidden bg-slate-950 text-slate-100 antialiased">
      {/* Responsive Sidebar */}
      <Sidebar onOpenNewChat={onOpenNewChat}>
        {sidebarContent}
      </Sidebar>

      {/* Main Chat Area */}
      <div className="flex flex-1 flex-col overflow-hidden">
        {activeConversationId ? (
          <>
            <ChatHeader
              conversation={activeConversation}
              currentUserId={user?.id}
              onOpenInfo={onOpenInfo}
            />
            <div className="flex flex-1 flex-col overflow-hidden bg-slate-950">
              {children}
            </div>
          </>
        ) : (
          <>
            <ChatHeader />
            <EmptyChatState />
          </>
        )}
      </div>
    </div>
  );
};
