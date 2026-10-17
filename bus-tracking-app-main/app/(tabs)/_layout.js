import React from 'react';
import { Tabs } from 'expo-router';
import { View, Text } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useNotifications } from '../../context/NotificationContext';
import Colors from '../../constants/Colors';

export default function TabLayout() {
  const { unreadCount } = useNotifications();
  return <Tabs screenOptions={{ headerStyle: { backgroundColor: '#FFF' }, headerShadowVisible: false,
    headerTitleStyle: { color: Colors.text, fontSize: 16, fontWeight: '800' },
    headerTitle: () => <View style={{ flexDirection: 'row', alignItems: 'center', gap: 10 }}><View style={{ padding: 8, borderRadius: 10, backgroundColor: Colors.primary }}><Ionicons name="bus" size={18} color="#FFF" /></View><Text style={{ fontSize: 15, fontWeight: '800', letterSpacing: 0.7, color: Colors.navy }}>BUS TRACK <Text style={{ color: Colors.primary }}>PRO</Text></Text></View>,
    tabBarActiveTintColor: Colors.primary, tabBarInactiveTintColor: Colors.textSecondary,
    tabBarStyle: { borderTopColor: Colors.border, backgroundColor: '#FFF', minHeight: 66, paddingTop: 9 }, tabBarLabelStyle: { fontSize: 11, fontWeight: '600', marginTop: 2 } }}>
    <Tabs.Screen name="home" options={{ title: 'Overview', tabBarIcon: ({ color }) => <Ionicons name="grid-outline" size={22} color={color} /> }} />
    <Tabs.Screen name="alerts" options={{ title: 'Alerts', tabBarBadge: unreadCount || undefined, tabBarBadgeStyle: { backgroundColor: Colors.error, fontSize: 10 }, tabBarIcon: ({ color }) => <Ionicons name="notifications-outline" size={22} color={color} /> }} />
    <Tabs.Screen name="settings" options={{ title: 'Settings', tabBarIcon: ({ color }) => <Ionicons name="options-outline" size={22} color={color} /> }} />
  </Tabs>;
}
