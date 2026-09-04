import React, { useState } from 'react';
import type { Message, User } from '@/types';
import { formatMessageTime } from '@/lib/dateUtils';
import {
  Reply,
  Pencil,
  Trash2,
  Check,
  X,
  User as UserIcon,
  CornerDownRight,
} from 'lucide-react';

interface MessageItemProps {
  message: Message;
  currentUser: User | null;
  onReply: (message: Message) => void;
  onEdit: (messageId: string, newContent: string) => Promise<void>;
  onDelete: (messageId: string) => Promise<void>;
}

export const MessageItem: React.FC<MessageItemProps> = ({
  message,
  currentUser,
  onReply,
  onEdit,
  onDelete,
}) => {
  const isOwn = (message.senderId || message.sender?.id) === currentUser?.id;
  const isDeleted = message.deleted;

  const [isEditing, setIsEditing] = useState(false);
  const [editContent, setEditContent] = useState(message.content || '');
  const [isSaving, setIsSaving] = useState(false);

  const handleSaveEdit = async () => {
    if (!editContent.trim() || editContent === message.content) {
      setIsEditing(false);
      return;
    }

    try {
      setIsSaving(true);
      await onEdit(message.id, editContent.trim());
      setIsEditing(false);
    } catch (err) {
      console.error('Failed to edit message:', err);
    } finally {
      setIsSaving(false);
    }
  };

  const handleDelete = async () => {
    if (window.confirm('Are you sure you want to delete this message?')) {
      try {
        await onDelete(message.id);
      } catch (err) {
        console.error('Failed to delete message:', err);
      }
    }
  };

  return (
    <div
      className={`group relative flex w-full gap-3 px-4 py-1.5 transition-colors duration-150 hover:bg-slate-100/60 dark:hover:bg-slate-900/40 ${
        isOwn ? 'flex-row-reverse' : 'flex-row'
      }`}
    >
      {/* Avatar (for received messages or group chats) */}
      {!isOwn && (
        <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-slate-200 text-indigo-600 ring-1 ring-slate-300 dark:bg-slate-800 dark:text-indigo-400 dark:ring-slate-700/50">
          {message.sender?.avatarUrl ? (
            <img
              src={message.sender.avatarUrl}
              alt={message.sender.displayName || message.sender.username}
              className="h-full w-full rounded-full object-cover"
            />
          ) : (
            <UserIcon className="h-4 w-4 text-slate-500 dark:text-slate-400" />
          )}
        </div>
      )}

      {/* Message Bubble Container */}
      <div
        className={`flex max-w-[75%] flex-col md:max-w-[65%] ${
          isOwn ? 'items-end' : 'items-start'
        }`}
      >
        {/* Sender Name (for incoming messages) */}
        {!isOwn && (
          <span className="mb-1 text-[11px] font-semibold text-slate-600 dark:text-slate-400">
            {message.sender?.displayName || message.sender?.username || 'Unknown'}
          </span>
        )}

        {/* Replying Banner (if message is a reply) */}
        {message.replyTo && (
          <div
            className={`mb-1 flex items-center gap-1.5 rounded-lg border px-2.5 py-1 text-xs ${
              isOwn
                ? 'border-blue-400/40 bg-blue-600/30 text-blue-100 dark:border-blue-500/40 dark:bg-blue-950/50 dark:text-blue-200'
                : 'border-slate-300 bg-slate-100 text-slate-700 dark:border-slate-700 dark:bg-slate-800/60 dark:text-slate-300'
            }`}
          >
            <CornerDownRight className="h-3 w-3 shrink-0 text-slate-400" />
            <span className="font-semibold text-slate-700 dark:text-slate-300">
              {message.replyTo.sender?.displayName ||
                message.replyTo.sender?.username ||
                'User'}
              :
            </span>
            <span className="truncate text-slate-500 dark:text-slate-400">
              {message.replyTo.deleted
                ? 'Deleted message'
                : message.replyTo.content}
            </span>
          </div>
        )}

        {/* Message Bubble & Content */}
        <div className="relative">
          {isEditing ? (
            <div className="flex flex-col gap-2 rounded-2xl border border-[#0084ff]/50 bg-white p-2.5 shadow-xl dark:bg-slate-900">
              <input
                type="text"
                value={editContent}
                onChange={(e) => setEditContent(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') handleSaveEdit();
                  if (e.key === 'Escape') setIsEditing(false);
                }}
                autoFocus
                className="w-full rounded-lg bg-slate-100 px-3 py-1.5 text-xs text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-1 focus:ring-[#0084ff] dark:bg-slate-950 dark:text-white dark:placeholder-slate-500"
              />
              <div className="flex justify-end gap-1.5">
                <button
                  onClick={() => setIsEditing(false)}
                  disabled={isSaving}
                  className="rounded px-2 py-1 text-[11px] text-slate-500 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white"
                >
                  <X className="h-3.5 w-3.5 inline mr-1" />
                  Cancel
                </button>
                <button
                  onClick={handleSaveEdit}
                  disabled={isSaving}
                  className="rounded bg-[#0084ff] px-2.5 py-1 text-[11px] font-semibold text-white transition hover:bg-[#0073e6]"
                >
                  <Check className="h-3.5 w-3.5 inline mr-1" />
                  Save
                </button>
              </div>
            </div>
          ) : (
            <div
              className={`rounded-2xl px-4 py-2.5 text-sm shadow-sm ${
                isDeleted
                  ? 'border border-slate-200 bg-slate-100/60 text-slate-400 italic dark:border-slate-800 dark:bg-slate-900/40 dark:text-slate-500'
                  : isOwn
                  ? 'bg-[#0084ff] text-white rounded-br-xs shadow-md shadow-[#0084ff]/25'
                  : 'bg-slate-200/90 text-slate-900 rounded-bl-xs border border-slate-300/80 dark:bg-slate-800 dark:text-slate-100 dark:border-slate-700/50'
              }`}
            >
              <p className="whitespace-pre-wrap break-words leading-relaxed">
                {isDeleted ? 'This message was deleted' : message.content}
              </p>

              {/* Timestamp & Edited badge */}
              <div
                className={`mt-1 flex items-center justify-end gap-1 text-[10px] ${
                  isOwn ? 'text-blue-100/90' : 'text-slate-500 dark:text-slate-400'
                }`}
              >
                {message.edited && !isDeleted && (
                  <span className="italic opacity-80">(edited)</span>
                )}
                <span>{formatMessageTime(message.createdAt)}</span>
              </div>
            </div>
          )}

          {/* Hover Action Menu */}
          {!isEditing && !isDeleted && (
            <div
              className={`absolute top-0 hidden -translate-y-1/2 items-center gap-0.5 rounded-lg border border-slate-200 bg-white/95 p-0.5 shadow-lg backdrop-blur-sm dark:border-slate-700 dark:bg-slate-800/95 group-hover:flex ${
                isOwn ? 'right-full mr-2' : 'left-full ml-2'
              }`}
            >
              <button
                onClick={() => onReply(message)}
                title="Reply"
                className="flex h-7 w-7 items-center justify-center rounded text-slate-500 transition hover:bg-slate-100 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-slate-700 dark:hover:text-white"
              >
                <Reply className="h-3.5 w-3.5" />
              </button>

              {isOwn && (
                <>
                  <button
                    onClick={() => {
                      setEditContent(message.content || '');
                      setIsEditing(true);
                    }}
                    title="Edit"
                    className="flex h-7 w-7 items-center justify-center rounded text-slate-500 transition hover:bg-slate-100 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-slate-700 dark:hover:text-white"
                  >
                    <Pencil className="h-3.5 w-3.5" />
                  </button>

                  <button
                    onClick={handleDelete}
                    title="Delete"
                    className="flex h-7 w-7 items-center justify-center rounded text-slate-500 transition hover:bg-red-500/15 hover:text-red-600 dark:text-slate-400 dark:hover:bg-red-500/20 dark:hover:text-red-400"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                </>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
