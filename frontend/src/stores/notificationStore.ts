import { create } from 'zustand';

export type NotificationType = 'success' | 'error' | 'warning' | 'info';

export interface Notification {
  id: string;
  type: NotificationType;
  message: string;
  duration?: number;
  action?: {
    label: string;
    onClick: () => void;
  };
}

interface NotificationState {
  notifications: Notification[];
  soundEnabled: boolean;
  desktopEnabled: boolean;
  hasPermission: boolean;
}

interface NotificationActions {
  addNotification: (notification: Omit<Notification, 'id'>) => void;
  removeNotification: (id: string) => void;
  clearAll: () => void;

  setSoundEnabled: (enabled: boolean) => void;
  setDesktopEnabled: (enabled: boolean) => void;
  requestPermission: () => Promise<boolean>;

  showDesktopNotification: (title: string, body: string, icon?: string) => void;
}

type NotificationStore = NotificationState & NotificationActions;

export const useNotificationStore = create<NotificationStore>((set, get) => ({
  // State
  notifications: [],
  soundEnabled: true,
  desktopEnabled: false,
  hasPermission: false,

  // Actions
  addNotification: (notification) => {
    const id = `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    const newNotification: Notification = {
      ...notification,
      id,
      duration: notification.duration ?? 5000,
    };

    set((state) => ({
      notifications: [...state.notifications, newNotification],
    }));

    // 자동 제거
    if (newNotification.duration && newNotification.duration > 0) {
      setTimeout(() => {
        get().removeNotification(id);
      }, newNotification.duration);
    }

    // 사운드 재생
    if (get().soundEnabled && notification.type !== 'info') {
      // 브라우저에서 간단한 알림 사운드 재생 (선택적)
      try {
        const audio = new Audio('/sounds/notification.mp3');
        audio.volume = 0.5;
        audio.play().catch(() => { /* 무시 */ });
      } catch {
        // 오디오 파일이 없으면 무시
      }
    }
  },

  removeNotification: (id: string) => {
    set((state) => ({
      notifications: state.notifications.filter(n => n.id !== id),
    }));
  },

  clearAll: () => {
    set({ notifications: [] });
  },

  setSoundEnabled: (enabled: boolean) => {
    set({ soundEnabled: enabled });
  },

  setDesktopEnabled: (enabled: boolean) => {
    set({ desktopEnabled: enabled });
  },

  requestPermission: async () => {
    if (!('Notification' in window)) {
      return false;
    }

    if (Notification.permission === 'granted') {
      set({ hasPermission: true, desktopEnabled: true });
      return true;
    }

    if (Notification.permission !== 'denied') {
      const permission = await Notification.requestPermission();
      const granted = permission === 'granted';
      set({ hasPermission: granted, desktopEnabled: granted });
      return granted;
    }

    return false;
  },

  showDesktopNotification: (title: string, body: string, icon?: string) => {
    const { desktopEnabled, hasPermission } = get();

    if (!desktopEnabled || !hasPermission) {
      return;
    }

    if ('Notification' in window && Notification.permission === 'granted') {
      new Notification(title, {
        body,
        icon: icon || '/icons/icon-192x192.png',
        badge: '/icons/icon-72x72.png',
        tag: 'beam-notification',
      });
    }
  },
}));

// 편의 함수들
export const showSuccess = (message: string) => {
  useNotificationStore.getState().addNotification({ type: 'success', message });
};

export const showError = (message: string) => {
  useNotificationStore.getState().addNotification({ type: 'error', message, duration: 8000 });
};

export const showWarning = (message: string) => {
  useNotificationStore.getState().addNotification({ type: 'warning', message });
};

export const showInfo = (message: string) => {
  useNotificationStore.getState().addNotification({ type: 'info', message });
};
