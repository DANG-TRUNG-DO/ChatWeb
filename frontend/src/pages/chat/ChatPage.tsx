import React from 'react';
import { useAuthStore } from '@/stores/useAuthStore';
import { LogOut, MessageSquare, User } from 'lucide-react';

export const ChatPage: React.FC = () => {
  const { user, logout } = useAuthStore();

  return (
    <div className="flex h-screen w-full flex-col bg-slate-950 text-slate-100">
      {/* Top Navbar */}
      <header className="flex h-16 shrink-0 items-center justify-between border-b border-slate-800 bg-slate-900/60 px-6 backdrop-blur">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-600/20 text-indigo-400 ring-1 ring-indigo-500/30">
            <MessageSquare className="h-5 w-5" />
          </div>
          <div>
            <h1 className="text-base font-semibold text-white">ChatWeb</h1>
            <p className="text-xs text-slate-400">Realtime Chat Application</p>
          </div>
        </div>

        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2 text-sm text-slate-300">
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-slate-800 text-slate-300">
              <User className="h-4 w-4" />
            </div>
            <span>{user?.displayName || user?.username}</span>
          </div>

          <button
            onClick={() => logout()}
            className="flex items-center gap-2 rounded-lg border border-slate-700 bg-slate-800/80 px-3 py-1.5 text-xs font-medium text-slate-300 transition hover:bg-slate-700 hover:text-white"
          >
            <LogOut className="h-3.5 w-3.5" />
            <span>Logout</span>
          </button>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex flex-1 items-center justify-center p-6">
        <div className="max-w-md text-center">
          <h2 className="text-2xl font-bold text-white">Welcome, {user?.displayName || user?.username}!</h2>
          <p className="mt-2 text-sm text-slate-400">
            Your authentication session is active. Next tasks will implement Conversation sidebar, message list, and realtime chat area.
          </p>
        </div>
      </main>
    </div>
  );
};
