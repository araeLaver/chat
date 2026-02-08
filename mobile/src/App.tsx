import React, {useEffect} from 'react';
import {StatusBar, useColorScheme} from 'react-native';
import {NavigationContainer} from '@react-navigation/native';
import {SafeAreaProvider} from 'react-native-safe-area-context';
import {GestureHandlerRootView} from 'react-native-gesture-handler';
import {RootNavigator} from './navigation/RootNavigator';
import {useAuthStore} from './stores/authStore';
import {usePushNotification} from './hooks/usePushNotification';

function App(): React.JSX.Element {
  const isDarkMode = useColorScheme() === 'dark';
  const {checkAuth} = useAuthStore();
  const {requestPermission} = usePushNotification();

  useEffect(() => {
    checkAuth();
    requestPermission();
  }, [checkAuth, requestPermission]);

  return (
    <GestureHandlerRootView style={{flex: 1}}>
      <SafeAreaProvider>
        <NavigationContainer>
          <StatusBar
            barStyle={isDarkMode ? 'light-content' : 'dark-content'}
            backgroundColor={isDarkMode ? '#1e1e1e' : '#ffffff'}
          />
          <RootNavigator />
        </NavigationContainer>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}

export default App;
