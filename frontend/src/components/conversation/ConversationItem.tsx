import React from 'react';
import type { Conversation } from '@/types';
import { useAuthStore } from '@/stores/useAuthStore';
import { formatMessageTime } from '@/lib/dateUtils';
import { Users, User as UserIcon } from 'lucide-react';

interface ConversationItemProps {
  conversation: Conversation;
  isActive: boolean;
  onClick: () => void;
}

export const ConversationItem: React.FC<ConversationItemProps> = ({
  conversation,
  isActive,
  onClick,
}) => {
  const { user } = useAuthStore();

  const isDirect = conversation.type === 'DIRECT';
  const otherMember = isDirect
    ? conversation.members?.find((m) => m.userId !== user?.id)
    : null;
  const partner = conversation.partner;

  const title = isDirect
    ? (partner?.displayName || partner?.username || otherMember?.user?.displayName || otherMember?.user?.username || conversation.name || 'Direct Chat')
    : (conversation.name || 'Group Chat');

  const avatarUrl = isDirect
    ? (partner?.avatarUrl || otherMember?.user?.avatarUrl || conversation.avatarUrl)
    : conversation.avatarUrl;

  const lastMessage = conversation.lastMessage;
  const lastMessageText = lastMessage
    ? lastMessage.deleted
      ? 'This message was deleted'
      : lastMessage.content || 'Sent an attachment'
    : 'No messages yet';

  const timeDisplay = formatMessageTime(
    lastMessage?.createdAt || conversation.updatedAt || conversation.createdAt
  );

  return (
    <button
      onClick={onClick}
      className={`group relative flex w-full items-center gap-3 rounded-xl p-3 text-left transition-all duration-150 ${
        isActive
          ? 'bg-indigo-50 text-indigo-900 ring-1 ring-indigo-500/40 dark:bg-indigo-600/15 dark:text-white dark:ring-indigo-500/30'
          : 'hover:bg-slate-100 dark:hover:bg-slate-800/60'
      }`}
    >
      {/* Avatar */}
      <div className="relative flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-slate-200 text-indigo-600 ring-1 ring-slate-300 dark:bg-slate-800 dark:text-indigo-400 dark:ring-slate-700/50">
        {avatarUrl ? (
          <img
            src={avatarUrl}
            alt={title}
            className="h-full w-full rounded-full object-cover"
          />
        ) : isDirect ? (
          <UserIcon className="h-6 w-6 text-slate-500 dark:text-slate-400" />
        ) : (
          <Users className="h-6 w-6 text-slate-500 dark:text-slate-400" />
        )}
      </div>

      {/* Content & Details */}
      <div className="min-w-0 flex-1">
        <div className="flex items-center justify-between gap-1">
          <h3
            className={`truncate text-sm font-semibold ${
              isActive
                ? 'text-indigo-950 font-bold dark:text-white'
                : 'text-slate-900 dark:text-slate-200'
            }`}
          >
            {title}
          </h3>
          {timeDisplay && (
            <span
              className={`shrink-0 text-[11px] ${
                conversation.unreadCount > 0
                  ? 'font-semibold text-indigo-600 dark:text-indigo-400'
                  : 'text-slate-400 dark:text-slate-500'
              }`}
            >
              {timeDisplay}
            </span>
          )}
        </div>

        <div className="mt-1 flex items-center justify-between gap-2">
          <p
            className={`truncate text-xs ${
              conversation.unreadCount > 0
                ? 'font-medium text-slate-900 dark:text-slate-200'
                : 'text-slate-500 dark:text-slate-400'
            }`}
          >
            {lastMessageText}
          </p>

          {conversation.unreadCount > 0 && (
            <span className="flex h-5 min-w-[20px] shrink-0 items-center justify-center rounded-full bg-indigo-600 px-1.5 text-[11px] font-bold text-white shadow-sm shadow-indigo-600/40">
              {conversation.unreadCount > 99 ? '99+' : conversation.unreadCount}
            </span>
          )}
        </div>
      </div>
    </button>
  );
};
