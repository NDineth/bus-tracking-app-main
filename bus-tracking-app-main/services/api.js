import axios from 'axios';
import Config from '../constants/Config';
import * as Storage from '../utils/storage';
import { requestErrorMessage } from '../utils/connection';

const API = axios.create({ baseURL: Config.API_BASE_URL, timeout: 15000,
  headers: { 'Content-Type': 'application/json' } });
API.interceptors.request.use(async (config) => {
  // Public auth requests must not inherit a stale session's bearer token.
  if (config.url === '/auth/sign-in' || config.url === '/auth/signup') return config;
  const token = await Storage.getItemAsync('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
export const errorMessage = (error) => requestErrorMessage(error, Config.API_BASE_URL);
export default API;
