import { create } from 'zustand';

export type ThemeMode = 'light' | 'dark' | 'system';

interface ThemeState {
  theme: ThemeMode;
  resolvedTheme: 'light' | 'dark';
  setTheme: (theme: ThemeMode) => void;
  toggleTheme: () => void;
  initializeTheme: () => void;
}

const applyThemeToDOM = (theme: ThemeMode): 'light' | 'dark' => {
  const root = document.documentElement;
  const systemPrefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
  const isDark = theme === 'dark' || (theme === 'system' && systemPrefersDark);

  if (isDark) {
    root.classList.add('dark');
  } else {
    root.classList.remove('dark');
  }

  return isDark ? 'dark' : 'light';
};

export const useThemeStore = create<ThemeState>((set, get) => ({
  theme: (localStorage.getItem('chatweb_theme') as ThemeMode) || 'dark',
  resolvedTheme: 'dark',

  setTheme: (theme: ThemeMode) => {
    localStorage.setItem('chatweb_theme', theme);
    const resolvedTheme = applyThemeToDOM(theme);
    set({ theme, resolvedTheme });
  },

  toggleTheme: () => {
    const current = get().theme;
    const nextTheme: ThemeMode = current === 'dark' ? 'light' : 'dark';
    get().setTheme(nextTheme);
  },

  initializeTheme: () => {
    const savedTheme = (localStorage.getItem('chatweb_theme') as ThemeMode) || 'dark';
    const resolvedTheme = applyThemeToDOM(savedTheme);
    set({ theme: savedTheme, resolvedTheme });

    // Listen for system theme changes if set to system
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    const handleChange = () => {
      if (get().theme === 'system') {
        const resolved = applyThemeToDOM('system');
        set({ resolvedTheme: resolved });
      }
    };

    mediaQuery.addEventListener('change', handleChange);
  },
}));
