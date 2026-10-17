import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';

const NotificationItem = ({ notification, onRead }) => {
  const isRead = notification.read !== undefined ? notification.read : notification.isRead;
  const router = useRouter();

  const getIcon = (category) => {
    switch (category) {
      case 'BUS_DELAY': return 'time-outline';
      case 'ROUTE_CHANGE': return 'map-outline';
      case 'URGENT': return 'alert-circle-outline';
      default: return 'notifications-outline';
    }
  };

  const getIconColor = (category) => {
    switch (category) {
      case 'BUS_DELAY': return '#FF9500';
      case 'ROUTE_CHANGE': return '#007AFF';
      case 'URGENT': return '#FF3B30';
      default: return '#5856D6';
    }
  };

  const formatTime = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  const handlePress = () => {
    if (!isRead) {
      onRead(notification.notificationId);
    }
    if (notification.title && notification.title.toLowerCase().includes('approval request')) {
      router.push('/my-requests');
    }
  };

  return (
    <TouchableOpacity 
      style={[styles.container, !isRead && styles.unreadContainer]} 
      onPress={handlePress}
      activeOpacity={0.7}
    >
      <View style={[styles.iconBadge, { backgroundColor: getIconColor(notification.category) + '20' }]}>
        <Ionicons name={getIcon(notification.category)} size={24} color={getIconColor(notification.category)} />
      </View>
      
      <View style={styles.content}>
        <View style={styles.header}>
          <Text style={[styles.title, !isRead && styles.unreadText]}>{notification.title}</Text>
          <Text style={styles.time}>{formatTime(notification.createdAt)}</Text>
        </View>
        <Text style={styles.body} numberOfLines={2}>{notification.body}</Text>
      </View>

      {!isRead && <View style={styles.unreadIndicator} />}
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    padding: 16,
    backgroundColor: '#FFFFFF',
    borderRadius: 16,
    marginBottom: 12,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.05,
    shadowRadius: 8,
    elevation: 2,
  },
  unreadContainer: {
    backgroundColor: '#F8F9FF',
    borderLeftWidth: 4,
    borderLeftColor: '#007AFF',
  },
  iconBadge: {
    width: 48,
    height: 48,
    borderRadius: 12,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 16,
  },
  content: {
    flex: 1,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 4,
  },
  title: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1C1C1E',
    flex: 1,
  },
  unreadText: {
    color: '#000000',
  },
  time: {
    fontSize: 12,
    color: '#8E8E93',
    marginLeft: 8,
  },
  body: {
    fontSize: 14,
    color: '#3A3A3C',
    lineHeight: 20,
  },
  unreadIndicator: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#007AFF',
    marginLeft: 8,
  }
});

export default NotificationItem;
