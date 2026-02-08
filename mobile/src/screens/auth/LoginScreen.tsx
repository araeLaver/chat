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
} from 'react-native';
import type {NativeStackNavigationProp} from '@react-navigation/native-stack';
import type {AuthStackParamList} from '../../navigation';
import {useAuthStore} from '../../stores/authStore';
import {authApi} from '../../api';

type LoginScreenNavigationProp = NativeStackNavigationProp<
  AuthStackParamList,
  'Login'
>;

interface Props {
  navigation: LoginScreenNavigationProp;
}

export function LoginScreen({navigation}: Props) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const {login} = useAuthStore();

  const handleLogin = async () => {
    if (!email.trim() || !password.trim()) {
      Alert.alert('알림', '이메일과 비밀번호를 입력해주세요.');
      return;
    }

    setIsLoading(true);
    try {
      const response = await authApi.login({email, password});
      login(response.token, response.user);
    } catch (error: any) {
      const message =
        error.response?.data?.message ||
        '로그인에 실패했습니다. 다시 시도해주세요.';
      Alert.alert('로그인 실패', message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleAnonymousLogin = async () => {
    setIsLoading(true);
    try {
      const username = `게스트_${Math.random().toString(36).substring(7)}`;
      const response = await authApi.registerAnonymous({username});
      login(response.token, response.user);
    } catch (error: any) {
      Alert.alert('오류', '익명 로그인에 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
      <View style={styles.content}>
        <View style={styles.logoContainer}>
          <Text style={styles.logo}>BEAM</Text>
          <Text style={styles.subtitle}>Secure Global Messenger</Text>
        </View>

        <View style={styles.form}>
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
            placeholder="비밀번호"
            placeholderTextColor="#999"
            value={password}
            onChangeText={setPassword}
            secureTextEntry
          />

          <TouchableOpacity
            style={[styles.button, styles.primaryButton]}
            onPress={handleLogin}
            disabled={isLoading}>
            {isLoading ? (
              <ActivityIndicator color="#3C1E1E" />
            ) : (
              <Text style={styles.primaryButtonText}>로그인</Text>
            )}
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.button, styles.secondaryButton]}
            onPress={() => navigation.navigate('Register')}
            disabled={isLoading}>
            <Text style={styles.secondaryButtonText}>회원가입</Text>
          </TouchableOpacity>

          <View style={styles.divider}>
            <View style={styles.dividerLine} />
            <Text style={styles.dividerText}>또는</Text>
            <View style={styles.dividerLine} />
          </View>

          <TouchableOpacity
            style={[styles.button, styles.anonymousButton]}
            onPress={handleAnonymousLogin}
            disabled={isLoading}>
            <Text style={styles.anonymousButtonText}>익명으로 시작하기</Text>
          </TouchableOpacity>
        </View>
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FEE500',
  },
  content: {
    flex: 1,
    justifyContent: 'center',
    paddingHorizontal: 32,
  },
  logoContainer: {
    alignItems: 'center',
    marginBottom: 48,
  },
  logo: {
    fontSize: 48,
    fontWeight: 'bold',
    color: '#3C1E1E',
  },
  subtitle: {
    fontSize: 16,
    color: '#5C4848',
    marginTop: 8,
  },
  form: {
    gap: 12,
  },
  input: {
    backgroundColor: '#fff',
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
  },
  primaryButtonText: {
    color: '#FEE500',
    fontSize: 16,
    fontWeight: 'bold',
  },
  secondaryButton: {
    backgroundColor: 'transparent',
    borderWidth: 2,
    borderColor: '#3C1E1E',
  },
  secondaryButtonText: {
    color: '#3C1E1E',
    fontSize: 16,
    fontWeight: 'bold',
  },
  divider: {
    flexDirection: 'row',
    alignItems: 'center',
    marginVertical: 16,
  },
  dividerLine: {
    flex: 1,
    height: 1,
    backgroundColor: '#5C4848',
  },
  dividerText: {
    color: '#5C4848',
    paddingHorizontal: 16,
    fontSize: 14,
  },
  anonymousButton: {
    backgroundColor: '#fff',
  },
  anonymousButtonText: {
    color: '#666',
    fontSize: 16,
  },
});
