import React, { createContext, useState, useContext, useEffect, useCallback, useRef } from 'react';
import { AppState } from 'react-native';
import { useAuth } from './AuthContext';
import { getNotificationsByReceiverId, markAsRead } from '../services/notificationService';
import { isUnread, notificationId, sortNotifications } from '../utils/alertPolicy';
import { errorMessage } from '../services/api';

const NotificationContext = createContext();
export const useNotifications = () => useContext(NotificationContext);
export function NotificationProvider({ children }) {
  const { user } = useAuth();
  const [notifications, setNotifications] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [lastUpdated, setLastUpdated] = useState(null);
  const identity = useRef(user?.id);
  identity.current = user?.id;
  const pending = useRef(new Set());
  const acknowledged = useRef(new Set());
  const fetching = useRef(null);
  const fetchNotifications = useCallback(async () => {
    if (!user?.id || fetching.current === user.id) return;
    const receiverId = user.id;
    fetching.current = receiverId;
    setIsLoading(true);
    try {
      const data = await getNotificationsByReceiverId(receiverId);
      if (identity.current !== receiverId) return;
      setNotifications(sortNotifications(data || []).map(n => acknowledged.current.has(notificationId(n))
        ? { ...n, isRead: true, read: true } : n));
      setError(null); setLastUpdated(new Date());
    } catch (e) {
      if (identity.current === receiverId) setError(errorMessage(e));
    } finally {
      if (fetching.current === receiverId) fetching.current = null;
      if (identity.current === receiverId) setIsLoading(false);
    }
  }, [user?.id]);
  useEffect(() => {
    setNotifications([]); setError(null); setLastUpdated(null); setIsLoading(false);
    acknowledged.current = new Set(); pending.current = new Set();
    fetchNotifications();
    const interval = setInterval(() => {
      if (AppState.currentState === 'active') fetchNotifications();
    }, 30000);
    const subscription = AppState.addEventListener('change', state => {
      if (state === 'active') fetchNotifications();
    });
    return () => { clearInterval(interval); subscription.remove(); };
  }, [fetchNotifications]);
  const markNotificationRead = async (id) => {
    if (!user?.id || id == null || pending.current.has(id)) return;
    const receiverId = user.id;
    pending.current.add(id);
    try {
      await markAsRead(id, receiverId);
      if (identity.current !== receiverId) return;
      acknowledged.current.add(id);
      setNotifications(previous => previous.map(n => notificationId(n) === id ? { ...n, isRead: true, read: true } : n));
    } catch (e) {
      if (identity.current === receiverId) setError(errorMessage(e));
      throw e;
    } finally { if (identity.current === receiverId) pending.current.delete(id); }
  };
  const markAllRead = async () => {
    const results = await Promise.allSettled(notifications.filter(isUnread).map(n => markNotificationRead(notificationId(n))));
    if (results.some(result => result.status === 'rejected')) throw new Error('Some alerts could not be marked read. Please retry.');
  };
  return <NotificationContext.Provider value={{ notifications, unreadCount: notifications.filter(isUnread).length,
    isLoading, error, lastUpdated, refreshNotifications: fetchNotifications, markNotificationRead, markAllRead }}>{children}</NotificationContext.Provider>;
}
