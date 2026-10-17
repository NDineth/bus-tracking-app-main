import React from 'react';
import { FlatList, RefreshControl } from 'react-native';
import NotificationItem from './NotificationItem';
import { EmptyState, ui } from './UI';
import { notificationId } from '../utils/alertPolicy';
import Colors from '../constants/Colors';

export default function NotificationList({ notifications, onRefresh, refreshing, onMarkRead, header, filtered = false }) {
  return <FlatList data={notifications} keyExtractor={item => String(notificationId(item))}
    renderItem={({ item }) => <NotificationItem notification={item} onRead={onMarkRead} />}
    contentContainerStyle={ui.content} ListHeaderComponent={header}
    ListEmptyComponent={<EmptyState icon="notifications-outline" title={refreshing ? 'Loading your alerts…' : filtered ? 'No matching alerts' : 'You’re all caught up'}
      description={filtered ? 'Try another filter or search term.' : 'Safety alerts and request updates appear here. Pull down to refresh.'} />}
    refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={Colors.primary} />} />;
}
