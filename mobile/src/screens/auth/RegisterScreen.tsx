import React, {useState} from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  Alert,
  ActivityIndicator,
  ScrollView,
} from 'react-native';
import type {NativeStackNavigationProp} from '@react-navigation/native-stack';
import type {AuthStackParamList} from '../../navigation';
import {useAuthStore} from '../../stores/authStore';
import {authApi} from '../../api';

type RegisterScreenNavigationProp = NativeStackNavigationProp<
  AuthStackParamList,
  'Register'
>;

interface Props {
  navigation: RegisterScreenNavigationProp;
}

export function RegisterScreen({navigation}: Props) {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const {login} = useAuthStore();

  const handleRegister = async () => {
    if (!username.trim() || !email.trim() || !password.trim()) {
      Alert.alert('알림', '모든 필드를 입력해주세요.');
      return;
    }

    if (password !== confirmPassword) {
      Alert.alert('알림', '비밀번호가 일치하지 않습니다.');
      return;
    }

    if (password.length < 6) {
      Alert.alert('알림', '비밀번호는 6자 이상이어야 합니다.');
      return;
    }

    setIsLoading(true);
    try {
      const response = await authApi.register({username, email, password});
      login(response.token, response.user);
    } catch (error: any) {
      const message =
        error.response?.data?.message ||
        '회원가입에 실패했습니다. 다시 시도해주세요.';
      Alert.alert('회원가입 실패', message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
      <ScrollView
        contentContainerStyle={styles.scrollContent}
        keyboardShouldPersistTaps="handled">
        <View style={styles.form}>
          <Text style={styles.title}>BEAM 가입하기</Text>
          <Text style={styles.subtitle}>
            안전한 글로벌 메신저에 오신 것을 환영합니다
          </Text>

          <TextInput
            style={styles.input}
            placeholder="사용자 이름"
            placeholderTextColor="#999"
            value={username}
            onChangeText={setUsername}
            autoCapitalize="none"
            autoCorrect={false}
          />

          <TextInput
            style={styles.input}
            placeholder="이메일"
            placeholderTextColor="#999"
            value={email}
            onChangeText={setEmail}
            keyboardType="email-address"
            autoCapitalize="none"
            autoCorrect={false}
          />

          <TextInput
            style={styles.input}
            placeholder="비밀번호 (6자 이상)"
            placeholderTextColor="#999"
            value={password}
            onChangeText={setPassword}
            secureTextEntry
          />

          <TextInput
            style={styles.input}
            placeholder="비밀번호 확인"
            placeholderTextColor="#999"
            value={confirmPassword}
            onChangeText={setConfirmPassword}
            secureTextEntry
          />

          <TouchableOpacity
            style={[styles.button, styles.primaryButton]}
            onPress={handleRegister}
            disabled={isLoading}>
            {isLoading ? (
              <ActivityIndicator color="#FEE500" />
            ) : (
              <Text style={styles.primaryButtonText}>가입하기</Text>
            )}
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.button, styles.linkButton]}
            onPress={() => navigation.goBack()}
            disabled={isLoading}>
            <Text style={styles.linkButtonText}>
              이미 계정이 있으신가요? 로그인
            </Text>
          </TouchableOpacity>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  scrollContent: {
    flexGrow: 1,
    justifyContent: 'center',
    paddingHorizontal: 32,
    paddingVertical: 24,
  },
  form: {
    gap: 12,
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#3C1E1E',
    textAlign: 'center',
    marginBottom: 4,
  },
  subtitle: {
    fontSize: 14,
    color: '#666',
    textAlign: 'center',
    marginBottom: 24,
  },
  input: {
    backgroundColor: '#f5f5f5',
    borderRadius: 12,
    padding: 16,
    fontSize: 16,
    color: '#333',
  },
  button: {
    borderRadius: 12,
    padding: 16,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 8,
  },
  primaryButton: {
    backgroundColor: '#3C1E1E',
    marginTop: 16,
  },
  primaryButtonText: {
    color: '#FEE500',
    fontSize: 16,
    fontWeight: 'bold',
  },
  linkButton: {
    backgroundColor: 'transparent',
  },
  linkButtonText: {
    color: '#3C1E1E',
    fontSize: 14,
  },
});
