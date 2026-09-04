import React from 'react';
import type { User } from '@/types';
import { User as UserIcon, MessageSquarePlus } from 'lucide-react';

interface UserSearchResultItemProps {
  user: User;
  onClick: () => void;
  isLoading?: boolean;
}

export const UserSearchResultItem: React.FC<UserSearchResultItemProps> = ({
  user,
  onClick,
  isLoading,
}) => {
  return (
    <button
      onClick={onClick}
      disabled={isLoading}
      className="group flex w-full items-center justify-between gap-3 rounded-xl p-3 text-left transition-all duration-150 hover:bg-slate-100 disabled:opacity-50 dark:hover:bg-slate-800/60"
    >
      <div className="flex items-center gap-3 overflow-hidden">
        <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-slate-200 text-indigo-600 ring-1 ring-slate-300 dark:bg-slate-800 dark:text-indigo-400 dark:ring-slate-700/50">
          {user.avatarUrl ? (
            <img
              src={user.avatarUrl}
              alt={user.displayName || user.username}
              className="h-full w-full rounded-full object-cover"
            />
          ) : (
            <UserIcon className="h-5 w-5 text-slate-500 dark:text-slate-400" />
          )}
        </div>

        <div className="overflow-hidden">
          <p className="truncate text-sm font-semibold text-slate-900 group-hover:text-indigo-600 dark:text-slate-100 dark:group-hover:text-white">
            {user.displayName || user.username}
          </p>
          <p className="truncate text-xs text-slate-500 dark:text-slate-400">@{user.username}</p>
        </div>
      </div>

      <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-slate-400 transition group-hover:bg-indigo-600/15 group-hover:text-indigo-600 dark:text-slate-500 dark:group-hover:bg-indigo-600/20 dark:group-hover:text-indigo-400">
        <MessageSquarePlus className="h-4 w-4" />
      </div>
    </button>
  );
};
