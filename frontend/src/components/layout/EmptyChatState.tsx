import React from 'react';
import { useConversationStore } from '@/stores/useConversationStore';
import { MessageSquare, Users, ShieldCheck, Zap } from 'lucide-react';

export const EmptyChatState: React.FC = () => {
  const { toggleMobileSidebar } = useConversationStore();

  return (
    <div className="flex h-full flex-1 flex-col items-center justify-center bg-slate-950 p-6 text-center text-slate-100">
      <div className="flex max-w-sm flex-col items-center">
        <div className="flex h-16 w-16 items-center justify-center rounded-3xl bg-indigo-600/20 text-indigo-400 ring-1 ring-indigo-500/30">
          <MessageSquare className="h-8 w-8" />
        </div>

        <h3 className="mt-5 text-xl font-bold text-white tracking-tight">
          Select a Conversation
        </h3>
        <p className="mt-2 text-xs leading-relaxed text-slate-400">
          Choose an existing conversation from the sidebar or start a new direct chat with a user.
        </p>

        {/* Mobile Open Sidebar Button */}
        <button
          onClick={toggleMobileSidebar}
          className="mt-6 flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2.5 text-xs font-semibold text-white shadow-lg shadow-indigo-600/20 transition hover:bg-indigo-500 md:hidden"
        >
          <MessageSquare className="h-4 w-4" />
          <span>Open Conversations</span>
        </button>

        {/* Features badges */}
        <div className="mt-10 grid grid-cols-3 gap-3 text-center">
          <div className="rounded-xl border border-slate-800/80 bg-slate-900/40 p-3">
            <Zap className="mx-auto h-4 w-4 text-amber-400 mb-1" />
            <span className="text-[11px] font-medium text-slate-400">Realtime</span>
          </div>
          <div className="rounded-xl border border-slate-800/80 bg-slate-900/40 p-3">
            <ShieldCheck className="mx-auto h-4 w-4 text-emerald-400 mb-1" />
            <span className="text-[11px] font-medium text-slate-400">Secure JWT</span>
          </div>
          <div className="rounded-xl border border-slate-800/80 bg-slate-900/40 p-3">
            <Users className="mx-auto h-4 w-4 text-indigo-400 mb-1" />
            <span className="text-[11px] font-medium text-slate-400">Direct Chat</span>
          </div>
        </div>
      </div>
    </div>
  );
};
