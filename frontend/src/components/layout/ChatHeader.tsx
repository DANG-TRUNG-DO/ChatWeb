import React from 'react';
import { useConversationStore } from '@/stores/useConversationStore';
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

  if (!conversation) {
    return (
      <header className="flex h-16 shrink-0 items-center justify-between border-b border-slate-800 bg-slate-900/60 px-4 md:px-6">
        <div className="flex items-center gap-3">
          <button
            onClick={toggleMobileSidebar}
            className="flex h-9 w-9 items-center justify-center rounded-lg text-slate-400 hover:bg-slate-800 hover:text-white md:hidden"
          >
            <Menu className="h-5 w-5" />
          </button>
          <span className="text-sm font-medium text-slate-400">Select a conversation</span>
        </div>
      </header>
    );
  }

  // Get recipient info for direct chat
  const otherMember = conversation.type === 'DIRECT'
    ? conversation.members.find((m) => m.userId !== currentUserId)
    : null;

  const title = conversation.type === 'DIRECT'
    ? (otherMember?.user.displayName || otherMember?.user.username || 'Direct Chat')
    : (conversation.name || 'Group Chat');

  const avatar = conversation.type === 'DIRECT'
    ? otherMember?.user.avatarUrl
    : conversation.avatarUrl;

  const subtitle = conversation.type === 'DIRECT'
    ? (otherMember?.user.email || 'Direct Conversation')
    : `${conversation.members.length} members`;

  return (
    <header className="flex h-16 shrink-0 items-center justify-between border-b border-slate-800 bg-slate-900/60 px-4 backdrop-blur md:px-6">
      <div className="flex items-center gap-3 overflow-hidden">
        {/* Mobile Hamburger Button */}
        <button
          onClick={toggleMobileSidebar}
          className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg text-slate-400 transition hover:bg-slate-800 hover:text-white md:hidden"
        >
          <Menu className="h-5 w-5" />
        </button>

        {/* Conversation Avatar */}
        <div className="relative flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-indigo-600/20 text-indigo-400 ring-1 ring-indigo-500/30">
          {avatar ? (
            <img src={avatar} alt={title} className="h-full w-full rounded-full object-cover" />
          ) : conversation.type === 'GROUP' ? (
            <Users className="h-5 w-5" />
          ) : (
            <UserIcon className="h-5 w-5" />
          )}
        </div>

        {/* Conversation Title & Subtitle */}
        <div className="overflow-hidden">
          <h2 className="truncate text-sm font-bold text-white">{title}</h2>
          <p className="truncate text-[11px] text-slate-400">{subtitle}</p>
        </div>
      </div>

      {/* Header Actions */}
      <div className="flex items-center gap-1">
        {onOpenInfo && (
          <button
            onClick={onOpenInfo}
            className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 transition hover:bg-slate-800 hover:text-white"
          >
            <MoreVertical className="h-4 w-4" />
          </button>
        )}
      </div>
    </header>
  );
};
