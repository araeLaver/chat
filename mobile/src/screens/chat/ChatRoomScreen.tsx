import React, {useEffect, useState, useRef, useCallback} from 'react';
import {
  View,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  FlatList,
  ActivityIndicator,
} from 'react-native';
import type {NativeStackScreenProps} from '@react-navigation/native-stack';
import type {ChatStackParamList} from '../../navigation';
import {useChatStore} from '../../stores/chatStore';
import {useAuthStore} from '../../stores/authStore';
import {useWebSocket} from '../../hooks/useWebSocket';
import {messagesApi} from '../../api';
import {MessageBubble} from '../../components/chat/MessageBubble';
import {MessageInput} from '../../components/chat/MessageInput';
import {TypingIndicator} from '../../components/common/TypingIndicator';
import type {ChatMessage} from '../../types';

type Props = NativeStackScreenProps<ChatStackParamList, 'ChatRoom'>;

export function ChatRoomScreen({route}: Props) {
  const {roomId, roomName} = route.params;
  const [isLoadingHistory, setIsLoadingHistory] = useState(true);
  const flatListRef = useRef<FlatList>(null);

  const {messages, typingUsers, addMessage, setMessages} = useChatStore();
  const {user} = useAuthStore();
  const {sendMessage, isConnected, joinRoom, leaveRoom, sendTyping} =
    useWebSocket();

  const roomMessages = messages.get(roomId) || [];
  const roomTypingUsers = typingUsers.get(roomId) || [];

  useEffect(() => {
    loadHistory();
    joinRoom(roomId);

    return () => {
      leaveRoom(roomId);
    };
  }, [roomId]);

  const loadHistory = async () => {
    try {
      const history = await messagesApi.getMessages(roomId);
      setMessages(roomId, history);
    } catch (error) {
      console.error('Failed to load message history:', error);
    } finally {
      setIsLoadingHistory(false);
    }
  };

  const handleSend = useCallback(
    (content: string) => {
      if (!content.trim() || !user) return;

      const message: ChatMessage = {
        type: 'message',
        sender: user.username,
        content: content.trim(),
        timestamp: new Date().toISOString(),
        roomId,
        userId: user.id,
      };

      sendMessage(message);
      addMessage(roomId, message);

      // Scroll to bottom
      setTimeout(() => {
        flatListRef.current?.scrollToEnd({animated: true});
      }, 100);
    },
    [roomId, user, sendMessage, addMessage],
  );

  const handleTyping = useCallback(
    (isTyping: boolean) => {
      sendTyping(roomId, isTyping);
    },
    [roomId, sendTyping],
  );

  const renderMessage = ({item, index}: {item: ChatMessage; index: number}) => {
    const isOwn = item.sender === user?.username;
    const prevMessage = index > 0 ? roomMessages[index - 1] : null;
    const nextMessage =
      index < roomMessages.length - 1 ? roomMessages[index + 1] : null;

    const showAvatar =
      !isOwn && (!prevMessage || prevMessage.sender !== item.sender);
    const showTime =
      !nextMessage ||
      nextMessage.sender !== item.sender ||
      new Date(nextMessage.timestamp).getTime() -
        new Date(item.timestamp).getTime() >
        60000;

    return (
      <MessageBubble
        message={item}
        isOwn={isOwn}
        showAvatar={showAvatar}
        showTime={showTime}
      />
    );
  };

  if (isLoadingHistory) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#3C1E1E" />
      </View>
    );
  }

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      keyboardVerticalOffset={Platform.OS === 'ios' ? 90 : 0}>
      <FlatList
        ref={flatListRef}
        data={roomMessages}
        renderItem={renderMessage}
        keyExtractor={(item, index) =>
          item.id?.toString() || `msg-${index}-${item.timestamp}`
        }
        contentContainerStyle={styles.messageList}
        onContentSizeChange={() =>
          flatListRef.current?.scrollToEnd({animated: false})
        }
        onLayout={() => flatListRef.current?.scrollToEnd({animated: false})}
      />

      {roomTypingUsers.length > 0 && (
        <TypingIndicator users={roomTypingUsers} />
      )}

      <MessageInput onSend={handleSend} onTyping={handleTyping} />
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#f5f5f5',
  },
  messageList: {
    paddingHorizontal: 12,
    paddingVertical: 8,
  },
});
