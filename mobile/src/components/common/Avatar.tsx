import React from 'react';
import {View, Text, StyleSheet, Image} from 'react-native';

interface AvatarProps {
  name: string;
  imageUrl?: string;
  size?: number;
}

function getInitials(name: string): string {
  if (!name) return '?';
  const parts = name.trim().split(/\s+/);
  if (parts.length >= 2) {
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }
  return name.slice(0, 2).toUpperCase();
}

function getColorFromName(name: string): string {
  const colors = [
    '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4',
    '#FFEAA7', '#DDA0DD', '#98D8C8', '#F7DC6F',
    '#BB8FCE', '#85C1E9', '#F8B500', '#FF8C94',
  ];

  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }
  return colors[Math.abs(hash) % colors.length];
}

export function Avatar({name, imageUrl, size = 40}: AvatarProps) {
  const backgroundColor = getColorFromName(name);
  const fontSize = size * 0.4;

  if (imageUrl) {
    return (
      <Image
        source={{uri: imageUrl}}
        style={[styles.image, {width: size, height: size, borderRadius: size / 2}]}
      />
    );
  }

  return (
    <View
      style={[
        styles.container,
        {
          width: size,
          height: size,
          borderRadius: size / 2,
          backgroundColor,
        },
      ]}>
      <Text style={[styles.initials, {fontSize}]}>{getInitials(name)}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    justifyContent: 'center',
    alignItems: 'center',
  },
  initials: {
    color: '#fff',
    fontWeight: 'bold',
  },
  image: {
    resizeMode: 'cover',
  },
});
