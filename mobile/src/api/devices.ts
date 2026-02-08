import apiClient from './axios';
import {Platform} from 'react-native';

export interface DeviceTokenRequest {
  deviceToken: string;
  deviceType: 'IOS' | 'ANDROID' | 'WEB';
  deviceName?: string;
}

export interface NotificationPreferences {
  pushEnabled: boolean;
  messagePreview: boolean;
  soundEnabled: boolean;
  vibrationEnabled: boolean;
  quietHoursStart?: string;
  quietHoursEnd?: string;
}

export const devicesApi = {
  registerDevice: async (token: string, deviceName?: string): Promise<void> => {
    const request: DeviceTokenRequest = {
      deviceToken: token,
      deviceType: Platform.OS === 'ios' ? 'IOS' : 'ANDROID',
      deviceName: deviceName || `${Platform.OS} Device`,
    };
    await apiClient.post('/api/v1/devices', request);
  },

  unregisterDevice: async (token: string): Promise<void> => {
    await apiClient.delete(`/api/v1/devices/${encodeURIComponent(token)}`);
  },

  getDevices: async (): Promise<
    {id: number; deviceType: string; deviceName: string; isActive: boolean}[]
  > => {
    const response = await apiClient.get('/api/v1/devices');
    return response.data;
  },

  getNotificationPreferences: async (): Promise<NotificationPreferences> => {
    const response = await apiClient.get('/api/v1/notifications/preferences');
    return response.data;
  },

  updateNotificationPreferences: async (
    preferences: Partial<NotificationPreferences>,
  ): Promise<void> => {
    await apiClient.put('/api/v1/notifications/preferences', preferences);
  },

  logoutAllDevices: async (): Promise<void> => {
    await apiClient.post('/api/v1/devices/logout-all');
  },
};

export default devicesApi;
