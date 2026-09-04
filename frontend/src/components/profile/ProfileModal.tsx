import React, { useState, useEffect } from 'react';
import { useAuthStore } from '@/stores/useAuthStore';
import { useThemeStore, type ThemeMode } from '@/stores/useThemeStore';
import { useUpdateProfile } from '@/hooks/useUserQueries';
import {
  X,
  User as UserIcon,
  Mail,
  Sparkles,
  Camera,
  Check,
  AlertCircle,
  Loader2,
  Sun,
  Moon,
  Laptop,
} from 'lucide-react';

interface ProfileModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const ProfileModal: React.FC<ProfileModalProps> = ({
  isOpen,
  onClose,
}) => {
  const { user } = useAuthStore();
  const { theme, setTheme } = useThemeStore();
  const updateProfileMutation = useUpdateProfile();

  const [displayName, setDisplayName] = useState(user?.displayName || '');
  const [avatarUrl, setAvatarUrl] = useState(user?.avatarUrl || '');
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    if (user) {
      setDisplayName(user.displayName || '');
      setAvatarUrl(user.avatarUrl || '');
    }
  }, [user, isOpen]);

  if (!isOpen || !user) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccessMessage(null);

    try {
      await updateProfileMutation.mutateAsync({
        displayName: displayName.trim() || undefined,
        avatarUrl: avatarUrl.trim() || undefined,
      });
      setSuccessMessage('Profile updated successfully!');
      setTimeout(() => {
        setSuccessMessage(null);
      }, 3000);
    } catch (err: unknown) {
      console.error('Failed to update profile:', err);
      setError('Failed to update profile. Please try again.');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="relative w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl dark:border-slate-800 dark:bg-slate-900">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-200 pb-4 dark:border-slate-800">
          <div className="flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-600/15 text-indigo-600 dark:bg-indigo-600/20 dark:text-indigo-400">
              <Sparkles className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900 dark:text-white">Preferences & Profile</h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">Manage appearance and profile details</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-900 dark:hover:bg-slate-800 dark:hover:text-white"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Alerts */}
        {error && (
          <div className="mt-4 flex items-center gap-2.5 rounded-xl border border-red-500/20 bg-red-500/10 p-3 text-xs text-red-500 dark:text-red-400">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {successMessage && (
          <div className="mt-4 flex items-center gap-2.5 rounded-xl border border-emerald-500/20 bg-emerald-500/10 p-3 text-xs text-emerald-600 dark:text-emerald-400">
            <Check className="h-4 w-4 shrink-0" />
            <span>{successMessage}</span>
          </div>
        )}

        {/* Theme Settings Section */}
        <div className="mt-5 space-y-2 border-b border-slate-200 pb-5 dark:border-slate-800">
          <label className="block text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
            Theme Appearance
          </label>
          <div className="grid grid-cols-3 gap-2">
            {[
              { mode: 'light' as ThemeMode, label: 'Light', icon: Sun },
              { mode: 'dark' as ThemeMode, label: 'Dark', icon: Moon },
              { mode: 'system' as ThemeMode, label: 'System', icon: Laptop },
            ].map(({ mode, label, icon: Icon }) => (
              <button
                key={mode}
                type="button"
                onClick={() => setTheme(mode)}
                className={`flex items-center justify-center gap-2 rounded-xl border p-2.5 text-xs font-medium transition ${
                  theme === mode
                    ? 'border-indigo-600 bg-indigo-50 text-indigo-600 ring-1 ring-indigo-500/40 dark:border-indigo-500 dark:bg-indigo-950/50 dark:text-indigo-300'
                    : 'border-slate-200 bg-slate-50 text-slate-600 hover:bg-slate-100 dark:border-slate-800 dark:bg-slate-950/60 dark:text-slate-400 dark:hover:bg-slate-800/80 dark:hover:text-white'
                }`}
              >
                <Icon className="h-4 w-4" />
                <span>{label}</span>
              </button>
            ))}
          </div>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          {/* Avatar Preview & URL */}
          <div className="flex flex-col items-center gap-3">
            <div className="relative flex h-20 w-20 items-center justify-center rounded-full bg-slate-100 text-indigo-600 ring-2 ring-indigo-500/40 dark:bg-slate-800 dark:text-indigo-400">
              {avatarUrl ? (
                <img
                  src={avatarUrl}
                  alt={displayName || user.username}
                  className="h-full w-full rounded-full object-cover"
                  onError={() => {
                    // Fallback on broken image link
                  }}
                />
              ) : (
                <UserIcon className="h-10 w-10 text-slate-400" />
              )}
            </div>

            <div className="w-full">
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                Avatar Image URL
              </label>
              <div className="relative mt-1">
                <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3 text-slate-400 dark:text-slate-500">
                  <Camera className="h-4 w-4" />
                </div>
                <input
                  type="url"
                  value={avatarUrl}
                  onChange={(e) => setAvatarUrl(e.target.value)}
                  placeholder="https://example.com/avatar.png"
                  className="w-full rounded-xl border border-slate-200 bg-slate-50 py-2.5 pl-9 pr-3 text-xs text-slate-900 placeholder-slate-400 transition focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 dark:border-slate-800 dark:bg-slate-950/70 dark:text-white dark:placeholder-slate-500"
                />
              </div>
            </div>
          </div>

          {/* Username (Read only) */}
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              Username
            </label>
            <div className="relative mt-1">
              <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3 text-slate-400 dark:text-slate-500">
                <UserIcon className="h-4 w-4" />
              </div>
              <input
                type="text"
                value={user.username}
                disabled
                className="w-full rounded-xl border border-slate-200 bg-slate-100 py-2.5 pl-9 pr-3 text-xs text-slate-500 cursor-not-allowed opacity-80 dark:border-slate-800/60 dark:bg-slate-950/40 dark:text-slate-400"
              />
            </div>
          </div>

          {/* Email (Read only) */}
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              Email Address
            </label>
            <div className="relative mt-1">
              <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3 text-slate-400 dark:text-slate-500">
                <Mail className="h-4 w-4" />
              </div>
              <input
                type="email"
                value={user.email}
                disabled
                className="w-full rounded-xl border border-slate-200 bg-slate-100 py-2.5 pl-9 pr-3 text-xs text-slate-500 cursor-not-allowed opacity-80 dark:border-slate-800/60 dark:bg-slate-950/40 dark:text-slate-400"
              />
            </div>
          </div>

          {/* Display Name (Editable) */}
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              Display Name
            </label>
            <div className="relative mt-1">
              <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3 text-slate-400 dark:text-slate-500">
                <Sparkles className="h-4 w-4" />
              </div>
              <input
                type="text"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                placeholder="Your display name"
                className="w-full rounded-xl border border-slate-200 bg-slate-50 py-2.5 pl-9 pr-3 text-xs text-slate-900 placeholder-slate-400 transition focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 dark:border-slate-800 dark:bg-slate-950/70 dark:text-white dark:placeholder-slate-500"
              />
            </div>
          </div>

          {/* Actions */}
          <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-200 dark:border-slate-800">
            <button
              type="button"
              onClick={onClose}
              className="rounded-xl px-4 py-2 text-xs font-medium text-slate-500 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-white"
            >
              Close
            </button>
            <button
              type="submit"
              disabled={updateProfileMutation.isPending}
              className="flex items-center gap-1.5 rounded-xl bg-indigo-600 px-4 py-2 text-xs font-semibold text-white shadow-lg shadow-indigo-600/30 transition hover:bg-indigo-500 disabled:opacity-50"
            >
              {updateProfileMutation.isPending ? (
                <>
                  <Loader2 className="h-3.5 w-3.5 animate-spin" />
                  <span>Saving...</span>
                </>
              ) : (
                <span>Save Changes</span>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
