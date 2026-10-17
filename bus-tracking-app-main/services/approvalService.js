import axios from 'axios';
import Config from '../constants/Config';
import * as SecureStore from 'expo-secure-store';

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

export const submitApprovalRequest = async (requestData) => {
  try {
    const response = await API.post('/approvals/request', requestData);
    return response.data;
  } catch (error) {
    throw error.response?.data?.message || 'Failed to submit approval request';
  }
};

export const getUserApprovalRequests = async (userId) => {
  try {
    const response = await API.get(`/approvals/user/${userId}`);
    return response.data;
  } catch (error) {
    throw error.response?.data?.message || 'Failed to fetch approval history';
  }
};

export default {
  submitApprovalRequest,
  getUserApprovalRequests,
};
