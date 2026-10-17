import React, { createContext, useState, useContext, useEffect, useCallback, useRef } from 'react';
import { Alert } from 'react-native';
import { useRouter } from 'expo-router';
import { useAuth } from './AuthContext';
import { getNotificationsByReceiverId, getUnreadNotificationsByReceiverId, markAsRead } from '../services/notificationService';
import { showError } from '../utils/alertService';


const NotificationContext = createContext();

export const useNotifications = () => useContext(NotificationContext);

export const NotificationProvider = ({ children }) => {
  const { user } = useAuth();
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const lastNotificationIdRef = useRef(null);
  const router = useRouter();

  const fetchNotifications = useCallback(async () => {
    if (!user?.id) return;
    
    setIsLoading(true);
    try {
      const data = await getNotificationsByReceiverId(user.id);
      setNotifications(data || []);
      const unread = (data || []).filter(n => !(n.read !== undefined ? n.read : n.isRead)).length;
      setUnreadCount(unread);

      // Check for new approval notifications
      if (data && data.length > 0) {
        const sorted = [...data].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
        const latest = sorted[0];
        const latestId = latest.notificationId || latest.id;
        
        if (lastNotificationIdRef.current !== null && latestId !== lastNotificationIdRef.current) {
          const isLatestUnread = !(latest.read !== undefined ? latest.read : latest.isRead);
          if (isLatestUnread && latest.title && latest.title.toLowerCase().includes('approval request')) {
            Alert.alert(
              latest.title,
              latest.body,
              [
                { text: 'Dismiss', style: 'cancel' },
                { 
                  text: 'View Requests', 
                  onPress: () => router.push('/my-requests') 
                }
              ]
            );
          }
        }
        lastNotificationIdRef.current = latestId;
      }
    } catch (error) {
      console.log('Failed to fetch notifications:', error);
    } finally {
      setIsLoading(false);
    }
  }, [user?.id, router]);

  const fetchUnreadCountOnly = useCallback(async () => {
    if (!user?.id) return;

    try {
      const data = await getUnreadNotificationsByReceiverId(user.id);
      setUnreadCount(data.length);
    } catch (error) {
      console.log('Failed to fetch unread count:', error);
    }
  }, [user?.id]);

  // Keep a mutable ref of the latest fetchNotifications callback to prevent stale closures in setInterval
  const fetchNotificationsRef = useRef(fetchNotifications);
  useEffect(() => {
    fetchNotificationsRef.current = fetchNotifications;
  }, [fetchNotifications]);

  useEffect(() => {
    if (user?.id) {
      fetchNotificationsRef.current();
      
      // Poll every 30 seconds
      const interval = setInterval(() => {
        fetchNotificationsRef.current();
      }, 30000);

      return () => clearInterval(interval);
    }
  }, [user?.id]);

  const markNotificationRead = async (notificationId) => {
    if (!user?.id) return;

    try {
      await markAsRead(notificationId, user.id);
      // Update local state to be responsive
      setNotifications(prev => 
        prev.map(n => n.notificationId === notificationId ? { ...n, read: true, isRead: true } : n)
      );
      setUnreadCount(prev => Math.max(0, prev - 1));
    } catch (error) {
      showError('Notifications', 'Failed to mark notification as read');
    }
  };

  return (
    <NotificationContext.Provider value={{ 
      notifications, 
      unreadCount, 
      isLoading, 
      refreshNotifications: fetchNotifications,
      markNotificationRead 
    }}>
      {children}
    </NotificationContext.Provider>
  );
};
