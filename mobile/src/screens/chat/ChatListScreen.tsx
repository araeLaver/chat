import React, {useEffect, useState} from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  RefreshControl,
  ActivityIndicator,
} from 'react-native';
import type {NativeStackNavigationProp} from '@react-navigation/native-stack';
import type {ChatStackParamList} from '../../navigation';
import {roomsApi} from '../../api';
import type {ChatRoom} from '../../types';
import {Avatar} from '../../components/common/Avatar';
import {formatDistanceToNow} from 'date-fns';
import {ko} from 'date-fns/locale';

type ChatListScreenNavigationProp = NativeStackNavigationProp<
  ChatStackParamList,
  'ChatList'
>;

interface Props {
  navigation: ChatListScreenNavigationProp;
}

export function ChatListScreen({navigation}: Props) {
  const [rooms, setRooms] = useState<ChatRoom[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const fetchRooms = async () => {
    try {
      const response = await roomsApi.getRooms();
      setRooms(response.rooms || []);
    } catch (error) {
      console.error('Failed to fetch rooms:', error);
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    fetchRooms();
  }, []);

  const handleRefresh = () => {
    setIsRefreshing(true);
    fetchRooms();
  };

  const handleRoomPress = (room: ChatRoom) => {
    navigation.navigate('ChatRoom', {roomId: room.id, roomName: room.name});
  };

  const formatTime = (timestamp?: string) => {
    if (!timestamp) return '';
    try {
      return formatDistanceToNow(new Date(timestamp), {
        addSuffix: true,
        locale: ko,
      });
    } catch {
      return '';
    }
  };

  const renderRoom = ({item}: {item: ChatRoom}) => (
    <TouchableOpacity
      style={styles.roomItem}
      onPress={() => handleRoomPress(item)}>
      <Avatar name={item.name} size={50} />
      <View style={styles.roomInfo}>
        <View style={styles.roomHeader}>
          <Text style={styles.roomName} numberOfLines={1}>
            {item.name}
          </Text>
          <Text style={styles.roomTime}>{formatTime(item.lastMessageTime)}</Text>
        </View>
        <View style={styles.roomFooter}>
          <Text style={styles.lastMessage} numberOfLines={1}>
            {item.lastMessage || '메시지가 없습니다'}
          </Text>
          {item.unreadCount ? (
            <View style={styles.unreadBadge}>
              <Text style={styles.unreadCount}>
                {item.unreadCount > 99 ? '99+' : item.unreadCount}
              </Text>
            </View>
          ) : null}
        </View>
      </View>
    </TouchableOpacity>
  );

  if (isLoading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#3C1E1E" />
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <FlatList
        data={rooms}
        renderItem={renderRoom}
        keyExtractor={item => item.id}
        contentContainerStyle={styles.list}
        refreshControl={
          <RefreshControl
            refreshing={isRefreshing}
            onRefresh={handleRefresh}
            tintColor="#3C1E1E"
          />
        }
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <Text style={styles.emptyText}>채팅방이 없습니다</Text>
            <Text style={styles.emptySubtext}>
              새로운 채팅을 시작해보세요
            </Text>
          </View>
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#fff',
  },
  list: {
    flexGrow: 1,
  },
  roomItem: {
    flexDirection: 'row',
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  roomInfo: {
    flex: 1,
    marginLeft: 12,
    justifyContent: 'center',
  },
  roomHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 4,
  },
  roomName: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    flex: 1,
  },
  roomTime: {
    fontSize: 12,
    color: '#999',
    marginLeft: 8,
  },
  roomFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  lastMessage: {
    fontSize: 14,
    color: '#666',
    flex: 1,
  },
  unreadBadge: {
    backgroundColor: '#FF3B30',
    borderRadius: 10,
    minWidth: 20,
    height: 20,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 6,
    marginLeft: 8,
  },
  unreadCount: {
    color: '#fff',
    fontSize: 12,
    fontWeight: 'bold',
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 64,
  },
  emptyText: {
    fontSize: 18,
    color: '#333',
    marginBottom: 8,
  },
  emptySubtext: {
    fontSize: 14,
    color: '#999',
  },
});
