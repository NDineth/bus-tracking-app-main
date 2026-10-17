import React from 'react';
import { View, Text, TouchableOpacity } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import AlertCenter from '../components/AlertCenter';
import { ui } from '../components/UI';
import Colors from '../constants/Colors';

export default function NotificationsScreen() {
  const router = useRouter();
  return <SafeAreaView style={ui.page}><View style={{ flexDirection: 'row', paddingHorizontal: 20, alignItems: 'center' }}>
    <TouchableOpacity accessibilityLabel="Back to dashboard" onPress={() => router.canGoBack() ? router.back() : router.replace('/(tabs)/home')} style={ui.linkButton}><Ionicons name="arrow-back" size={22} color={Colors.text} /><Text style={ui.link}>Dashboard</Text></TouchableOpacity></View><AlertCenter /></SafeAreaView>;
}
