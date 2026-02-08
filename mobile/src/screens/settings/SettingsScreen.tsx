import React, {useState, useEffect} from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  Switch,
  Alert,
} from 'react-native';
import {useAuthStore} from '../../stores/authStore';
import {devicesApi} from '../../api';
import {Avatar} from '../../components/common/Avatar';

export function SettingsScreen() {
  const {user, logout, themeMode, toggleTheme} = useAuthStore();
  const [pushEnabled, setPushEnabled] = useState(true);
  const [messagePreview, setMessagePreview] = useState(true);
  const [soundEnabled, setSoundEnabled] = useState(true);

  useEffect(() => {
    loadPreferences();
  }, []);

  const loadPreferences = async () => {
    try {
      const prefs = await devicesApi.getNotificationPreferences();
      setPushEnabled(prefs.pushEnabled);
      setMessagePreview(prefs.messagePreview);
      setSoundEnabled(prefs.soundEnabled);
    } catch (error) {
      console.error('Failed to load preferences:', error);
    }
  };

  const updatePreference = async (
    key: string,
    value: boolean,
    setter: (value: boolean) => void,
  ) => {
    setter(value);
    try {
      await devicesApi.updateNotificationPreferences({[key]: value});
    } catch (error) {
      setter(!value); // Revert on error
      console.error('Failed to update preference:', error);
    }
  };

  const handleLogout = () => {
    Alert.alert('로그아웃', '정말 로그아웃 하시겠습니까?', [
      {text: '취소', style: 'cancel'},
      {
        text: '로그아웃',
        style: 'destructive',
        onPress: logout,
      },
    ]);
  };

  const renderSection = (title: string, children: React.ReactNode) => (
    <View style={styles.section}>
      <Text style={styles.sectionTitle}>{title}</Text>
      <View style={styles.sectionContent}>{children}</View>
    </View>
  );

  const renderSwitchRow = (
    label: string,
    value: boolean,
    onValueChange: (value: boolean) => void,
  ) => (
    <View style={styles.row}>
      <Text style={styles.rowLabel}>{label}</Text>
      <Switch
        value={value}
        onValueChange={onValueChange}
        trackColor={{false: '#ddd', true: '#FEE500'}}
        thumbColor={value ? '#3C1E1E' : '#f4f3f4'}
      />
    </View>
  );

  return (
    <ScrollView style={styles.container}>
      {/* Profile Section */}
      <View style={styles.profileSection}>
        <Avatar name={user?.username || 'User'} size={80} />
        <Text style={styles.profileName}>
          {user?.displayName || user?.username}
        </Text>
        {user?.email && <Text style={styles.profileEmail}>{user.email}</Text>}
      </View>

      {/* Notification Settings */}
      {renderSection(
        '알림 설정',
        <>
          {renderSwitchRow('푸시 알림', pushEnabled, value =>
            updatePreference('pushEnabled', value, setPushEnabled),
          )}
          {renderSwitchRow('메시지 미리보기', messagePreview, value =>
            updatePreference('messagePreview', value, setMessagePreview),
          )}
          {renderSwitchRow('알림 소리', soundEnabled, value =>
            updatePreference('soundEnabled', value, setSoundEnabled),
          )}
        </>,
      )}

      {/* Display Settings */}
      {renderSection(
        '화면 설정',
        <>
          {renderSwitchRow('다크 모드', themeMode === 'dark', toggleTheme)}
        </>,
      )}

      {/* Account Actions */}
      {renderSection(
        '계정',
        <>
          <TouchableOpacity style={styles.row} onPress={handleLogout}>
            <Text style={styles.logoutText}>로그아웃</Text>
          </TouchableOpacity>
        </>,
      )}

      {/* App Info */}
      <View style={styles.appInfo}>
        <Text style={styles.appVersion}>BEAM v1.0.0</Text>
        <Text style={styles.appCopyright}>Secure Global Messenger</Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  profileSection: {
    backgroundColor: '#FEE500',
    paddingVertical: 32,
    alignItems: 'center',
  },
  profileName: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#3C1E1E',
    marginTop: 12,
  },
  profileEmail: {
    fontSize: 14,
    color: '#5C4848',
    marginTop: 4,
  },
  section: {
    marginTop: 16,
  },
  sectionTitle: {
    fontSize: 12,
    color: '#999',
    paddingHorizontal: 16,
    paddingVertical: 8,
    textTransform: 'uppercase',
  },
  sectionContent: {
    backgroundColor: '#fff',
    borderTopWidth: 1,
    borderBottomWidth: 1,
    borderColor: '#e0e0e0',
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  rowLabel: {
    fontSize: 16,
    color: '#333',
  },
  logoutText: {
    fontSize: 16,
    color: '#FF3B30',
  },
  appInfo: {
    padding: 32,
    alignItems: 'center',
  },
  appVersion: {
    fontSize: 14,
    color: '#999',
  },
  appCopyright: {
    fontSize: 12,
    color: '#ccc',
    marginTop: 4,
  },
});
