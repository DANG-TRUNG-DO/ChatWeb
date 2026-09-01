import React, { useState, useRef, useEffect } from 'react';
import type { Message } from '@/types';
import { Send, X, CornerDownRight } from 'lucide-react';

interface MessageInputProps {
  onSendMessage: (content: string, replyToId?: string) => Promise<void>;
  replyingTo: Message | null;
  onCancelReply: () => void;
  onTyping?: (typing: boolean) => void;
  disabled?: boolean;
}

export const MessageInput: React.FC<MessageInputProps> = ({
  onSendMessage,
  replyingTo,
  onCancelReply,
  onTyping,
  disabled = false,
}) => {
  const [content, setContent] = useState('');
  const [isSending, setIsSending] = useState(false);
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const typingTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (replyingTo && inputRef.current) {
      inputRef.current.focus();
    }
  }, [replyingTo]);

  const handleSend = async () => {
    if (!content.trim() || isSending || disabled) return;

    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }
    if (onTyping) onTyping(false);

    try {
      setIsSending(true);
      const text = content.trim();
      setContent('');
      await onSendMessage(text, replyingTo ? replyingTo.id : undefined);
      onCancelReply();
      if (inputRef.current) {
        inputRef.current.style.height = 'auto';
      }
    } catch (err) {
      console.error('Failed to send message:', err);
    } finally {
      setIsSending(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleInput = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setContent(e.target.value);
    e.target.style.height = 'auto';
    e.target.style.height = `${Math.min(e.target.scrollHeight, 120)}px`;

    if (onTyping) {
      onTyping(true);
      if (typingTimeoutRef.current) {
        clearTimeout(typingTimeoutRef.current);
      }
      typingTimeoutRef.current = setTimeout(() => {
        onTyping(false);
      }, 2000);
    }
  };

  return (
    <div className="border-t border-slate-800 bg-slate-900/80 p-3 backdrop-blur md:p-4">
      {/* Reply Banner */}
      {replyingTo && (
        <div className="mb-2 flex items-center justify-between rounded-xl border border-indigo-500/30 bg-indigo-950/40 px-3 py-2 text-xs text-indigo-300">
          <div className="flex items-center gap-2 overflow-hidden">
            <CornerDownRight className="h-4 w-4 shrink-0 text-indigo-400" />
            <span className="font-semibold text-slate-200">
              Replying to {replyingTo.sender?.displayName || replyingTo.sender?.username}:
            </span>
            <span className="truncate text-slate-400">
              {replyingTo.deleted ? 'Deleted message' : replyingTo.content}
            </span>
          </div>
          <button
            onClick={onCancelReply}
            className="ml-2 rounded-md p-1 text-slate-400 hover:bg-indigo-900/40 hover:text-white"
          >
            <X className="h-3.5 w-3.5" />
          </button>
        </div>
      )}

      {/* Input controls */}
      <div className="flex items-end gap-2">
        <div className="relative flex flex-1 items-center rounded-2xl border border-slate-800 bg-slate-950/80 px-3 py-1.5 focus-within:border-indigo-500 focus-within:ring-1 focus-within:ring-indigo-500">
          <textarea
            ref={inputRef}
            rows={1}
            value={content}
            onChange={handleInput}
            onKeyDown={handleKeyDown}
            disabled={disabled || isSending}
            placeholder="Type a message... (Enter to send, Shift+Enter for new line)"
            className="max-h-32 min-h-[24px] w-full resize-none bg-transparent py-1 text-xs text-white placeholder-slate-500 focus:outline-none"
          />
        </div>

        <button
          onClick={handleSend}
          disabled={!content.trim() || isSending || disabled}
          className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-indigo-600 text-white shadow-lg shadow-indigo-600/30 transition hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:opacity-40"
        >
          <Send className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
};
