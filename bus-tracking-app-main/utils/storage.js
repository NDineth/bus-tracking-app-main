import { Platform } from 'react-native';
import * as SecureStore from 'expo-secure-store';

// SecureStore is native-only. Browser sessions use origin-scoped storage.
export const getItemAsync = async (key) => Platform.OS === 'web'
  ? (typeof window === 'undefined' ? null : window.localStorage.getItem(key))
  : SecureStore.getItemAsync(key);
export const setItemAsync = async (key, value) => Platform.OS === 'web'
  ? window.localStorage.setItem(key, value)
  : SecureStore.setItemAsync(key, value);
export const deleteItemAsync = async (key) => Platform.OS === 'web'
  ? window.localStorage.removeItem(key)
  : SecureStore.deleteItemAsync(key);
