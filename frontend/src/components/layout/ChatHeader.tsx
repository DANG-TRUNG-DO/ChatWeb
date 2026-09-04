import React from 'react';
import { useConversationStore } from '@/stores/useConversationStore';
import { useWebSocketStore } from '@/stores/useWebSocketStore';
import type { Conversation } from '@/types';
import { Menu, User as UserIcon, MoreVertical, Users } from 'lucide-react';

interface ChatHeaderProps {
  conversation?: Conversation | null;
  currentUserId?: string;
  onOpenInfo?: () => void;
}

export const ChatHeader: React.FC<ChatHeaderProps> = ({
  conversation,
  currentUserId,
  onOpenInfo,
}) => {
  const { toggleMobileSidebar } = useConversationStore();
  const { isConnected, typingUsers } = useWebSocketStore();

  const activeTyping = conversation ? typingUsers[conversation.id] || [] : [];
  const isSomeoneTyping = activeTyping.length > 0;
  const typingText = isSomeoneTyping
    ? `${activeTyping.map((u) => u.username).join(', ')} is typing...`
    : null;

  if (!conversation) {
    return (
      <header className="flex h-16 shrink-0 items-center justify-between border-b border-slate-200 bg-white/80 px-4 backdrop-blur dark:border-slate-800 dark:bg-slate-900/60 md:px-6">
        <div className="flex items-center gap-3">
          <button
            onClick={toggleMobileSidebar}
            className="flex h-9 w-9 items-center justify-center rounded-lg text-slate-500 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-white md:hidden"
          >
            <Menu className="h-5 w-5" />
          </button>
          <span className="text-sm font-medium text-slate-500 dark:text-slate-400">Select a conversation</span>
        </div>

        {/* Realtime indicator */}
        <div className="flex items-center gap-2">
          <span
            className={`h-2 w-2 rounded-full ${
              isConnected ? 'bg-emerald-500 shadow-sm shadow-emerald-500/50' : 'bg-amber-500 animate-pulse'
            }`}
            title={isConnected ? 'Realtime Connected' : 'Connecting to Realtime...'}
          />
          <span className="text-[11px] text-slate-500">
            {isConnected ? 'Live' : 'Connecting...'}
          </span>
        </div>
      </header>
    );
  }

  // Get recipient info for direct chat
  const otherMember = conversation.type === 'DIRECT'
    ? conversation.members?.find((m) => m.userId !== currentUserId)
    : null;

  const partner = conversation.partner;

  const title = conversation.type === 'DIRECT'
    ? (partner?.displayName || partner?.username || otherMember?.user?.displayName || otherMember?.user?.username || conversation.name || 'Direct Chat')
    : (conversation.name || 'Group Chat');

  const avatar = conversation.type === 'DIRECT'
    ? (partner?.avatarUrl || otherMember?.user?.avatarUrl || conversation.avatarUrl)
    : conversation.avatarUrl;

  const subtitle = isSomeoneTyping
    ? typingText
    : conversation.type === 'DIRECT'
    ? (partner?.email || otherMember?.user?.email || 'Direct Conversation')
    : `${conversation.members?.length ?? 0} members`;

  return (
    <header className="flex h-16 shrink-0 items-center justify-between border-b border-slate-200 bg-white/80 px-4 backdrop-blur dark:border-slate-800 dark:bg-slate-900/60 md:px-6">
      <div className="flex items-center gap-3 overflow-hidden">
        {/* Mobile Hamburger Button */}
        <button
          onClick={toggleMobileSidebar}
          className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg text-slate-500 transition hover:bg-slate-100 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-white md:hidden"
        >
          <Menu className="h-5 w-5" />
        </button>

        {/* Conversation Avatar */}
        <div className="relative flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-indigo-600/15 text-indigo-600 ring-1 ring-indigo-500/30 dark:bg-indigo-600/20 dark:text-indigo-400">
          {avatar ? (
            <img src={avatar} alt={title} className="h-full w-full rounded-full object-cover" />
          ) : conversation.type === 'GROUP' ? (
            <Users className="h-5 w-5" />
          ) : (
            <UserIcon className="h-5 w-5" />
          )}

          {/* Connection status indicator */}
          <span
            className={`absolute bottom-0 right-0 h-3 w-3 rounded-full border-2 border-white dark:border-slate-900 ${
              isConnected ? 'bg-emerald-500' : 'bg-amber-500'
            }`}
          />
        </div>

        {/* Conversation Title & Subtitle */}
        <div className="overflow-hidden">
          <h2 className="truncate text-sm font-bold text-slate-900 dark:text-white">{title}</h2>
          <p
            className={`truncate text-[11px] ${
              isSomeoneTyping ? 'animate-pulse font-medium text-indigo-600 dark:text-indigo-400' : 'text-slate-500 dark:text-slate-400'
            }`}
          >
            {subtitle}
          </p>
        </div>
      </div>

      {/* Header Actions */}
      <div className="flex items-center gap-2">
        <div className="hidden items-center gap-1.5 sm:flex">
          <span
            className={`h-2 w-2 rounded-full ${
              isConnected ? 'bg-emerald-500 shadow-sm shadow-emerald-500/50' : 'bg-amber-500 animate-pulse'
            }`}
          />
          <span className="text-[11px] text-slate-500 dark:text-slate-400">
            {isConnected ? 'Realtime' : 'Connecting'}
          </span>
        </div>

        {onOpenInfo && (
          <button
            onClick={onOpenInfo}
            className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-500 transition hover:bg-slate-100 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-white"
          >
            <MoreVertical className="h-4 w-4" />
          </button>
        )}
      </div>
    </header>
  );
};
