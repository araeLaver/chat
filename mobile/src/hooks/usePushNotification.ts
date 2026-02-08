import {useEffect, useCallback} from 'react';
import {Platform, Alert, Linking} from 'react-native';
import messaging, {
  FirebaseMessagingTypes,
} from '@react-native-firebase/messaging';
import {devicesApi} from '../api';
import {useAuthStore} from '../stores/authStore';

export function usePushNotification() {
  const {isAuthenticated} = useAuthStore();

  const requestPermission = useCallback(async () => {
    try {
      const authStatus = await messaging().requestPermission();
      const enabled =
        authStatus === messaging.AuthorizationStatus.AUTHORIZED ||
        authStatus === messaging.AuthorizationStatus.PROVISIONAL;

      if (!enabled) {
        console.log('Push notification permission denied');
        return false;
      }

      console.log('Push notification permission granted');
      return true;
    } catch (error) {
      console.error('Failed to request permission:', error);
      return false;
    }
  }, []);

  const getToken = useCallback(async (): Promise<string | null> => {
    try {
      const token = await messaging().getToken();
      console.log('FCM Token:', token);
      return token;
    } catch (error) {
      console.error('Failed to get FCM token:', error);
      return null;
    }
  }, []);

  const registerDevice = useCallback(async () => {
    if (!isAuthenticated) return;

    const hasPermission = await requestPermission();
    if (!hasPermission) return;

    const token = await getToken();
    if (!token) return;

    try {
      await devicesApi.registerDevice(token);
      console.log('Device registered for push notifications');
    } catch (error) {
      console.error('Failed to register device:', error);
    }
  }, [isAuthenticated, requestPermission, getToken]);

  const unregisterDevice = useCallback(async () => {
    try {
      const token = await getToken();
      if (token) {
        await devicesApi.unregisterDevice(token);
        console.log('Device unregistered from push notifications');
      }
    } catch (error) {
      console.error('Failed to unregister device:', error);
    }
  }, [getToken]);

  // Handle foreground messages
  useEffect(() => {
    const unsubscribe = messaging().onMessage(
      async (remoteMessage: FirebaseMessagingTypes.RemoteMessage) => {
        console.log('Foreground message:', remoteMessage);

        // Show local notification or in-app notification
        if (remoteMessage.notification) {
          Alert.alert(
            remoteMessage.notification.title || 'New Message',
            remoteMessage.notification.body || '',
          );
        }
      },
    );

    return unsubscribe;
  }, []);

  // Handle background/quit messages when app is opened
  useEffect(() => {
    messaging()
      .getInitialNotification()
      .then((remoteMessage: FirebaseMessagingTypes.RemoteMessage | null) => {
        if (remoteMessage) {
          console.log(
            'App opened from quit state by notification:',
            remoteMessage,
          );
          handleNotificationNavigation(remoteMessage);
        }
      });

    const unsubscribe = messaging().onNotificationOpenedApp(
      (remoteMessage: FirebaseMessagingTypes.RemoteMessage) => {
        console.log('App opened from background by notification:', remoteMessage);
        handleNotificationNavigation(remoteMessage);
      },
    );

    return unsubscribe;
  }, []);

  // Handle token refresh
  useEffect(() => {
    const unsubscribe = messaging().onTokenRefresh(async (newToken: string) => {
      console.log('FCM Token refreshed:', newToken);
      if (isAuthenticated) {
        try {
          await devicesApi.registerDevice(newToken);
        } catch (error) {
          console.error('Failed to register refreshed token:', error);
        }
      }
    });

    return unsubscribe;
  }, [isAuthenticated]);

  // Register device when authenticated
  useEffect(() => {
    if (isAuthenticated) {
      registerDevice();
    }
  }, [isAuthenticated, registerDevice]);

  return {
    requestPermission,
    getToken,
    registerDevice,
    unregisterDevice,
  };
}

function handleNotificationNavigation(
  remoteMessage: FirebaseMessagingTypes.RemoteMessage,
) {
  const data = remoteMessage.data;
  if (data?.roomId) {
    // Navigate to the chat room
    // This would need to be handled by the navigation container
    console.log('Navigate to room:', data.roomId);
  }
}
