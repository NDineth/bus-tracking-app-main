import axios from 'axios';
import Config from '../constants/Config';
import * as SecureStore from 'expo-secure-store';
import { showError } from '../utils/alertService';


const API = axios.create({
  baseURL: Config.API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor to add token to requests
API.interceptors.request.use(async (config) => {
  const token = await SecureStore.getItemAsync('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const getNotificationsByReceiverId = async (receiverId) => {
  try {
    const response = await API.get(`/notifications/receiver/${receiverId}`);
    return (response.data || []).map(n => ({
      ...n,
      isRead: n.isRead !== undefined ? n.isRead : n.read
    }));
  } catch (error) {
    console.log('Error fetching notifications:', error);
    throw error.response?.data?.message || 'Failed to fetch notifications';
  }
};

export const getUnreadNotificationsByReceiverId = async (receiverId) => {
  try {
    const response = await API.get(`/notifications/receiver/${receiverId}/unread`);
    return (response.data || []).map(n => ({
      ...n,
      isRead: n.isRead !== undefined ? n.isRead : n.read
    }));
  } catch (error) {
    console.log('Error fetching unread notifications:', error);
    throw error.response?.data?.message || 'Failed to fetch unread notifications';
  }
};

export const markAsRead = async (notificationId, receiverId) => {
  try {
    const response = await API.patch(`/notifications/read/${notificationId}/receiver/${receiverId}`);
    return response.data;
  } catch (error) {
    console.log('Error marking notification as read:', error);
    throw error.response?.data?.message || 'Failed to mark notification as read';
  }
};

export default {
  getNotificationsByReceiverId,
  getUnreadNotificationsByReceiverId,
  markAsRead,
};
