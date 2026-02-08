import React from 'react';
import {View, Text, StyleSheet, Image} from 'react-native';
import {format} from 'date-fns';
import {Avatar} from '../common/Avatar';
import type {ChatMessage} from '../../types';

interface MessageBubbleProps {
  message: ChatMessage;
  isOwn: boolean;
  showAvatar: boolean;
  showTime: boolean;
}

export function MessageBubble({
  message,
  isOwn,
  showAvatar,
  showTime,
}: MessageBubbleProps) {
  const isSystem = message.type === 'system';
  const isImage = message.type === 'image' || message.mimeType?.startsWith('image/');
  const isFile = message.type === 'file' && !isImage;

  if (isSystem) {
    return (
      <View style={styles.systemContainer}>
        <Text style={styles.systemText}>{message.content}</Text>
      </View>
    );
  }

  const timeStr = format(new Date(message.timestamp), 'HH:mm');

  return (
    <View
      style={[
        styles.container,
        isOwn ? styles.ownContainer : styles.otherContainer,
      ]}>
      {!isOwn && showAvatar && (
        <View style={styles.avatarContainer}>
          <Avatar name={message.sender} size={32} />
        </View>
      )}
      {!isOwn && !showAvatar && <View style={styles.avatarPlaceholder} />}

      <View style={[styles.bubbleWrapper, isOwn && styles.ownBubbleWrapper]}>
        {!isOwn && showAvatar && (
          <Text style={styles.senderName}>{message.sender}</Text>
        )}

        <View
          style={[
            styles.bubble,
            isOwn ? styles.ownBubble : styles.otherBubble,
          ]}>
          {/* Reply preview */}
          {message.replyToId && (
            <View style={styles.replyPreview}>
              <Text style={styles.replyName}>{message.replyToSender}</Text>
              <Text style={styles.replyContent} numberOfLines={1}>
                {message.replyToContent}
              </Text>
            </View>
          )}

          {/* Image content */}
          {isImage && message.fileUrl && (
            <Image
              source={{uri: message.fileUrl}}
              style={styles.messageImage}
              resizeMode="cover"
            />
          )}

          {/* File content */}
          {isFile && (
            <View style={styles.fileContainer}>
              <Text style={styles.fileName}>{message.fileName}</Text>
              {message.fileSize && (
                <Text style={styles.fileSize}>
                  {(message.fileSize / 1024).toFixed(1)} KB
                </Text>
              )}
            </View>
          )}

          {/* Text content */}
          {message.content && (
            <Text
              style={[
                styles.messageText,
                isOwn ? styles.ownMessageText : styles.otherMessageText,
              ]}>
              {message.content}
            </Text>
          )}
        </View>

        {/* Time and read status */}
        {showTime && (
          <View
            style={[
              styles.metaContainer,
              isOwn && styles.ownMetaContainer,
            ]}>
            <Text style={styles.timeText}>{timeStr}</Text>
            {isOwn && message.isRead && (
              <Text style={styles.readStatus}>✓✓</Text>
            )}
          </View>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    marginVertical: 2,
    paddingHorizontal: 4,
  },
  ownContainer: {
    justifyContent: 'flex-end',
  },
  otherContainer: {
    justifyContent: 'flex-start',
  },
  avatarContainer: {
    marginRight: 8,
    alignSelf: 'flex-end',
  },
  avatarPlaceholder: {
    width: 32,
    marginRight: 8,
  },
  bubbleWrapper: {
    maxWidth: '70%',
  },
  ownBubbleWrapper: {
    alignItems: 'flex-end',
  },
  senderName: {
    fontSize: 12,
    color: '#666',
    marginBottom: 2,
    marginLeft: 4,
  },
  bubble: {
    borderRadius: 16,
    paddingHorizontal: 12,
    paddingVertical: 8,
    maxWidth: '100%',
  },
  ownBubble: {
    backgroundColor: '#FEE500',
    borderTopRightRadius: 4,
  },
  otherBubble: {
    backgroundColor: '#fff',
    borderTopLeftRadius: 4,
  },
  replyPreview: {
    backgroundColor: 'rgba(0,0,0,0.05)',
    borderLeftWidth: 2,
    borderLeftColor: '#3C1E1E',
    paddingHorizontal: 8,
    paddingVertical: 4,
    marginBottom: 4,
    borderRadius: 4,
  },
  replyName: {
    fontSize: 11,
    color: '#3C1E1E',
    fontWeight: 'bold',
  },
  replyContent: {
    fontSize: 11,
    color: '#666',
  },
  messageImage: {
    width: 200,
    height: 150,
    borderRadius: 8,
    marginBottom: 4,
  },
  fileContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 8,
    backgroundColor: 'rgba(0,0,0,0.05)',
    borderRadius: 8,
  },
  fileName: {
    fontSize: 14,
    color: '#333',
    fontWeight: '500',
  },
  fileSize: {
    fontSize: 12,
    color: '#999',
    marginLeft: 8,
  },
  messageText: {
    fontSize: 15,
    lineHeight: 20,
  },
  ownMessageText: {
    color: '#3C1E1E',
  },
  otherMessageText: {
    color: '#333',
  },
  metaContainer: {
    flexDirection: 'row',
    marginTop: 2,
    marginLeft: 4,
  },
  ownMetaContainer: {
    marginRight: 4,
    marginLeft: 0,
  },
  timeText: {
    fontSize: 10,
    color: '#999',
  },
  readStatus: {
    fontSize: 10,
    color: '#4A90D9',
    marginLeft: 4,
  },
  systemContainer: {
    alignItems: 'center',
    marginVertical: 8,
  },
  systemText: {
    fontSize: 12,
    color: '#999',
    backgroundColor: '#f0f0f0',
    paddingHorizontal: 12,
    paddingVertical: 4,
    borderRadius: 12,
  },
});
