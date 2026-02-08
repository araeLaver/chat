import React, {useState, useRef, useCallback} from 'react';
import {
  View,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  Platform,
} from 'react-native';
import {Text} from 'react-native';

interface MessageInputProps {
  onSend: (content: string) => void;
  onTyping?: (isTyping: boolean) => void;
  placeholder?: string;
}

export function MessageInput({
  onSend,
  onTyping,
  placeholder = '메시지를 입력하세요',
}: MessageInputProps) {
  const [text, setText] = useState('');
  const typingTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const isTypingRef = useRef(false);

  const handleTextChange = useCallback(
    (value: string) => {
      setText(value);

      if (onTyping) {
        // Start typing
        if (!isTypingRef.current && value.length > 0) {
          isTypingRef.current = true;
          onTyping(true);
        }

        // Clear existing timeout
        if (typingTimeoutRef.current) {
          clearTimeout(typingTimeoutRef.current);
        }

        // Stop typing after 2 seconds of inactivity
        typingTimeoutRef.current = setTimeout(() => {
          if (isTypingRef.current) {
            isTypingRef.current = false;
            onTyping(false);
          }
        }, 2000);
      }
    },
    [onTyping],
  );

  const handleSend = useCallback(() => {
    if (!text.trim()) return;

    onSend(text.trim());
    setText('');

    // Stop typing indicator
    if (onTyping && isTypingRef.current) {
      isTypingRef.current = false;
      onTyping(false);
    }

    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }
  }, [text, onSend, onTyping]);

  return (
    <View style={styles.container}>
      <View style={styles.inputContainer}>
        <TextInput
          style={styles.input}
          value={text}
          onChangeText={handleTextChange}
          placeholder={placeholder}
          placeholderTextColor="#999"
          multiline
          maxLength={2000}
        />
      </View>
      <TouchableOpacity
        style={[styles.sendButton, !text.trim() && styles.sendButtonDisabled]}
        onPress={handleSend}
        disabled={!text.trim()}>
        <Text style={styles.sendButtonText}>전송</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    paddingHorizontal: 8,
    paddingVertical: 8,
    backgroundColor: '#fff',
    borderTopWidth: 1,
    borderTopColor: '#f0f0f0',
  },
  inputContainer: {
    flex: 1,
    backgroundColor: '#f5f5f5',
    borderRadius: 20,
    paddingHorizontal: 16,
    paddingVertical: Platform.OS === 'ios' ? 10 : 6,
    marginRight: 8,
    maxHeight: 100,
  },
  input: {
    fontSize: 16,
    color: '#333',
    maxHeight: 80,
  },
  sendButton: {
    backgroundColor: '#FEE500',
    borderRadius: 20,
    paddingHorizontal: 16,
    paddingVertical: 10,
    justifyContent: 'center',
    alignItems: 'center',
  },
  sendButtonDisabled: {
    backgroundColor: '#f0f0f0',
  },
  sendButtonText: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#3C1E1E',
  },
});
