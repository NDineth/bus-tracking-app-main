import React from 'react';
import { FlatList, StyleSheet, View, Text, RefreshControl } from 'react-native';
import NotificationItem from './NotificationItem';

const NotificationList = ({ notifications, onRefresh, refreshing, onMarkRead }) => {
  if (!notifications || notifications.length === 0) {
    return (
      <View style={styles.emptyContainer}>
        <Text style={styles.emptyText}>No notifications yet</Text>
      </View>
    );
  }

  return (
    <FlatList
      data={notifications}
      keyExtractor={(item) => item.id.toString()}
      renderItem={({ item }) => (
        <NotificationItem 
          notification={item} 
          onRead={onMarkRead} 
        />
      )}
      contentContainerStyle={styles.listContent}
      refreshControl={
        <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor="#007AFF" />
      }
    />
  );
};

const styles = StyleSheet.create({
  listContent: {
    padding: 16,
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  emptyText: {
    fontSize: 16,
    color: '#8E8E93',
  }
});

export default NotificationList;
