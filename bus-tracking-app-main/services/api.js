import axios from 'axios';
import Config from '../constants/Config';
import * as Storage from '../utils/storage';

const API = axios.create({ baseURL: Config.API_BASE_URL, timeout: 15000,
  headers: { 'Content-Type': 'application/json' } });
API.interceptors.request.use(async (config) => {
  const token = await Storage.getItemAsync('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
export const errorMessage = (error) => error?.response?.data?.message
  || (error?.response?.data?.errors && Object.values(error.response.data.errors).join('. '))
  || (typeof error === 'string' ? error : error?.message)
  || 'Unable to connect. Please try again.';
export default API;
