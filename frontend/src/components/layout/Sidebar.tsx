import React, { useState } from 'react';
import { useAuthStore } from '@/stores/useAuthStore';
import { useConversationStore } from '@/stores/useConversationStore';
import { ProfileModal } from '@/components/profile/ProfileModal';
import {
  MessageSquare,
  Search,
  LogOut,
  X,
  Plus,
  User as UserIcon,
  Settings,
} from 'lucide-react';

interface SidebarProps {
  children?: React.ReactNode;
  onOpenNewChat?: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ children, onOpenNewChat }) => {
  const { user, logout } = useAuthStore();
  const {
    isMobileSidebarOpen,
    setMobileSidebarOpen,
    searchQuery,
    setSearchQuery,
  } = useConversationStore();

  const [isProfileModalOpen, setIsProfileModalOpen] = useState(false);

  return (
    <>
      {/* Mobile Backdrop Overlay */}
      {isMobileSidebarOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/60 backdrop-blur-sm transition-opacity md:hidden"
          onClick={() => setMobileSidebarOpen(false)}
        />
      )}

      {/* Profile Modal */}
      <ProfileModal
        isOpen={isProfileModalOpen}
        onClose={() => setIsProfileModalOpen(false)}
      />

      {/* Sidebar Main Container */}
      <aside
        className={`fixed inset-y-0 left-0 z-50 flex w-80 shrink-0 flex-col border-r border-slate-800 bg-slate-900/95 transition-transform duration-300 ease-in-out md:static md:w-84 md:translate-x-0 lg:w-96 ${
          isMobileSidebarOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {/* Top Header */}
        <div className="flex h-16 shrink-0 items-center justify-between border-b border-slate-800 px-4">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-600/20 text-indigo-400 ring-1 ring-indigo-500/30">
              <MessageSquare className="h-5 w-5" />
            </div>
            <div>
              <h1 className="text-base font-bold text-white tracking-tight">ChatWeb</h1>
              <p className="text-xs text-slate-400">Realtime Messenger</p>
            </div>
          </div>

          <div className="flex items-center gap-1">
            {onOpenNewChat && (
              <button
                onClick={onOpenNewChat}
                title="New Chat"
                className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 transition hover:bg-slate-800 hover:text-white"
              >
                <Plus className="h-4 w-4" />
              </button>
            )}

            {/* Mobile Close Button */}
            <button
              onClick={() => setMobileSidebarOpen(false)}
              className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 transition hover:bg-slate-800 hover:text-white md:hidden"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        {/* Search Bar */}
        <div className="border-b border-slate-800/80 p-3">
          <div className="relative">
            <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3 text-slate-500">
              <Search className="h-4 w-4" />
            </div>
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search chats or users..."
              className="w-full rounded-xl border border-slate-800 bg-slate-950/70 py-2 pl-9 pr-8 text-xs text-slate-200 placeholder-slate-500 transition focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="absolute inset-y-0 right-0 flex items-center pr-2.5 text-slate-500 hover:text-slate-300"
              >
                <X className="h-3.5 w-3.5" />
              </button>
            )}
          </div>
        </div>

        {/* Conversation List / Children Content */}
        <div className="flex-1 overflow-y-auto overflow-x-hidden p-2">
          {children ? (
            children
          ) : (
            <div className="flex h-full flex-col items-center justify-center p-4 text-center text-slate-500">
              <MessageSquare className="h-8 w-8 text-slate-600 mb-2" />
              <p className="text-xs">No conversations yet</p>
            </div>
          )}
        </div>

        {/* Bottom User Info & Profile/Logout Footer */}
        <div className="flex h-16 shrink-0 items-center justify-between border-t border-slate-800 bg-slate-950/40 px-4">
          <button
            onClick={() => setIsProfileModalOpen(true)}
            title="Edit Profile"
            className="flex items-center gap-3 overflow-hidden text-left rounded-xl p-1.5 transition hover:bg-slate-800/60"
          >
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-indigo-600/20 text-indigo-400 ring-1 ring-indigo-500/30">
              {user?.avatarUrl ? (
                <img
                  src={user.avatarUrl}
                  alt={user.displayName || user.username}
                  className="h-full w-full rounded-full object-cover"
                />
              ) : (
                <UserIcon className="h-4 w-4" />
              )}
            </div>
            <div className="overflow-hidden">
              <p className="truncate text-xs font-semibold text-white">
                {user?.displayName || user?.username}
              </p>
              <p className="truncate text-[11px] text-slate-400">@{user?.username}</p>
            </div>
          </button>

          <div className="flex items-center gap-1">
            <button
              onClick={() => setIsProfileModalOpen(true)}
              title="Profile Settings"
              className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 transition hover:bg-slate-800 hover:text-white"
            >
              <Settings className="h-4 w-4" />
            </button>

            <button
              onClick={() => logout()}
              title="Logout"
              className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 transition hover:bg-red-500/20 hover:text-red-400"
            >
              <LogOut className="h-4 w-4" />
            </button>
          </div>
        </div>
      </aside>
    </>
  );
};
