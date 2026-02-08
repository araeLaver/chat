import React, {useEffect, useRef} from 'react';
import {View, Text, StyleSheet, Animated, Easing} from 'react-native';
import type {TypingIndicator as TypingIndicatorType} from '../../types';

interface TypingIndicatorProps {
  users: TypingIndicatorType[];
}

function AnimatedDot({delay}: {delay: number}) {
  const opacity = useRef(new Animated.Value(0.3)).current;

  useEffect(() => {
    const animation = Animated.loop(
      Animated.sequence([
        Animated.timing(opacity, {
          toValue: 1,
          duration: 300,
          delay,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true,
        }),
        Animated.timing(opacity, {
          toValue: 0.3,
          duration: 300,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true,
        }),
      ]),
    );
    animation.start();

    return () => animation.stop();
  }, [delay, opacity]);

  return (
    <Animated.View style={[styles.dot, {opacity}]} />
  );
}

export function TypingIndicator({users}: TypingIndicatorProps) {
  if (users.length === 0) return null;

  const typingText =
    users.length === 1
      ? `${users[0].username}님이 입력 중`
      : `${users.length}명이 입력 중`;

  return (
    <View style={styles.container}>
      <Text style={styles.text}>{typingText}</Text>
      <View style={styles.dots}>
        <AnimatedDot delay={0} />
        <AnimatedDot delay={150} />
        <AnimatedDot delay={300} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 8,
    backgroundColor: '#fff',
    borderTopWidth: 1,
    borderTopColor: '#f0f0f0',
  },
  text: {
    fontSize: 12,
    color: '#666',
    fontStyle: 'italic',
  },
  dots: {
    flexDirection: 'row',
    marginLeft: 4,
  },
  dot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: '#3C1E1E',
    marginHorizontal: 2,
  },
});
