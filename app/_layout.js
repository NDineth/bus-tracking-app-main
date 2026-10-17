import { Slot, useRouter, useSegments } from 'expo-router';
import { AuthProvider, useAuth } from '../context/AuthContext';
import { NotificationProvider } from '../context/NotificationContext';
import { BusProvider } from '../context/BusContext';
import { useEffect } from 'react';
import { View, ActivityIndicator } from 'react-native';

function InitialLayout() {
  const { user, isLoading } = useAuth();
  const segments = useSegments();
  const router = useRouter();

  useEffect(() => {
    if (isLoading) return;

    const inAuthGroup = segments[0] === '(tabs)' || segments[0] === 'notifications' || segments[0] === 'allowed-buses' || segments[0] === 'my-requests' || segments[0] === 'new-request';
    const isAuthScreen = segments[0] === 'login' || segments[0] === 'register' || segments[0] === 'index' || !segments[0];

    if (!user && !isAuthScreen) {
      // Redirect to login if not logged in and trying to access protected screens
      router.replace('/login');
    } else if (user && isAuthScreen) {
      // Redirect to home if logged in and trying to access auth screens
      router.replace('/(tabs)/home');
    }
  }, [user, isLoading, segments]);

  if (isLoading) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  return <Slot />;
}

export default function RootLayout() {
  return (
    <AuthProvider>
      <NotificationProvider>
        <BusProvider>
          <InitialLayout />
        </BusProvider>
      </NotificationProvider>
    </AuthProvider>
  );
}
