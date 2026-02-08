import React from 'react';
import {createBottomTabNavigator} from '@react-navigation/bottom-tabs';
import {createNativeStackNavigator} from '@react-navigation/native-stack';
import {ChatListScreen} from '../screens/chat/ChatListScreen';
import {ChatRoomScreen} from '../screens/chat/ChatRoomScreen';
import {FriendsScreen} from '../screens/contacts/FriendsScreen';
import {SettingsScreen} from '../screens/settings/SettingsScreen';
import {Text, View} from 'react-native';

export type MainTabParamList = {
  ChatTab: undefined;
  FriendsTab: undefined;
  SettingsTab: undefined;
};

export type ChatStackParamList = {
  ChatList: undefined;
  ChatRoom: {roomId: string; roomName: string};
};

const Tab = createBottomTabNavigator<MainTabParamList>();
const ChatStack = createNativeStackNavigator<ChatStackParamList>();

function ChatNavigator() {
  return (
    <ChatStack.Navigator
      screenOptions={{
        headerStyle: {
          backgroundColor: '#FEE500',
        },
        headerTintColor: '#3C1E1E',
      }}>
      <ChatStack.Screen
        name="ChatList"
        component={ChatListScreen}
        options={{title: '채팅'}}
      />
      <ChatStack.Screen
        name="ChatRoom"
        component={ChatRoomScreen}
        options={({route}) => ({title: route.params.roomName})}
      />
    </ChatStack.Navigator>
  );
}

function TabIcon({name, focused}: {name: string; focused: boolean}) {
  const icons: Record<string, string> = {
    ChatTab: '💬',
    FriendsTab: '👥',
    SettingsTab: '⚙️',
  };

  return (
    <View style={{alignItems: 'center'}}>
      <Text style={{fontSize: 24}}>{icons[name]}</Text>
    </View>
  );
}

export function MainNavigator() {
  return (
    <Tab.Navigator
      screenOptions={({route}) => ({
        tabBarIcon: ({focused}) => (
          <TabIcon name={route.name} focused={focused} />
        ),
        tabBarActiveTintColor: '#3C1E1E',
        tabBarInactiveTintColor: '#999',
        tabBarStyle: {
          backgroundColor: '#FEE500',
          borderTopWidth: 0,
        },
        headerStyle: {
          backgroundColor: '#FEE500',
        },
        headerTintColor: '#3C1E1E',
      })}>
      <Tab.Screen
        name="ChatTab"
        component={ChatNavigator}
        options={{headerShown: false, tabBarLabel: '채팅'}}
      />
      <Tab.Screen
        name="FriendsTab"
        component={FriendsScreen}
        options={{title: '친구', tabBarLabel: '친구'}}
      />
      <Tab.Screen
        name="SettingsTab"
        component={SettingsScreen}
        options={{title: '설정', tabBarLabel: '설정'}}
      />
    </Tab.Navigator>
  );
}
