import React, {useEffect, useState} from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  RefreshControl,
  ActivityIndicator,
  TextInput,
} from 'react-native';
import {friendsApi} from '../../api';
import type {Friend} from '../../types';
import {Avatar} from '../../components/common/Avatar';

export function FriendsScreen() {
  const [friends, setFriends] = useState<Friend[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  const fetchFriends = async () => {
    try {
      const response = await friendsApi.getFriends();
      setFriends(response);
    } catch (error) {
      console.error('Failed to fetch friends:', error);
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    fetchFriends();
  }, []);

  const handleRefresh = () => {
    setIsRefreshing(true);
    fetchFriends();
  };

  const filteredFriends = friends.filter(
    friend =>
      friend.username.toLowerCase().includes(searchQuery.toLowerCase()) ||
      friend.displayName?.toLowerCase().includes(searchQuery.toLowerCase()),
  );

  const renderFriend = ({item}: {item: Friend}) => (
    <TouchableOpacity style={styles.friendItem}>
      <View style={styles.avatarContainer}>
        <Avatar name={item.displayName || item.username} size={50} />
        {item.isOnline && <View style={styles.onlineIndicator} />}
      </View>
      <View style={styles.friendInfo}>
        <Text style={styles.friendName}>
          {item.displayName || item.username}
        </Text>
        {item.displayName && (
          <Text style={styles.friendUsername}>@{item.username}</Text>
        )}
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
      <View style={styles.searchContainer}>
        <TextInput
          style={styles.searchInput}
          placeholder="친구 검색"
          placeholderTextColor="#999"
          value={searchQuery}
          onChangeText={setSearchQuery}
        />
      </View>

      <FlatList
        data={filteredFriends}
        renderItem={renderFriend}
        keyExtractor={item => item.id.toString()}
        contentContainerStyle={styles.list}
        refreshControl={
          <RefreshControl
            refreshing={isRefreshing}
            onRefresh={handleRefresh}
            tintColor="#3C1E1E"
          />
        }
        ListHeaderComponent={
          <Text style={styles.sectionHeader}>
            친구 {filteredFriends.length}명
          </Text>
        }
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <Text style={styles.emptyText}>
              {searchQuery ? '검색 결과가 없습니다' : '친구가 없습니다'}
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
  searchContainer: {
    padding: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  searchInput: {
    backgroundColor: '#f5f5f5',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    color: '#333',
  },
  list: {
    flexGrow: 1,
  },
  sectionHeader: {
    fontSize: 12,
    color: '#999',
    paddingHorizontal: 16,
    paddingVertical: 8,
    backgroundColor: '#f5f5f5',
  },
  friendItem: {
    flexDirection: 'row',
    padding: 12,
    paddingHorizontal: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  avatarContainer: {
    position: 'relative',
  },
  onlineIndicator: {
    position: 'absolute',
    bottom: 2,
    right: 2,
    width: 12,
    height: 12,
    borderRadius: 6,
    backgroundColor: '#4CD964',
    borderWidth: 2,
    borderColor: '#fff',
  },
  friendInfo: {
    flex: 1,
    marginLeft: 12,
    justifyContent: 'center',
  },
  friendName: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
  },
  friendUsername: {
    fontSize: 14,
    color: '#999',
    marginTop: 2,
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 64,
  },
  emptyText: {
    fontSize: 16,
    color: '#999',
  },
});
