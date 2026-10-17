import React, { createContext, useState, useContext, useEffect } from 'react';
import * as SecureStore from '../utils/storage';
import { loginUser, registerUser } from '../services/authService';
import { showError } from '../utils/alertService';


const AuthContext = createContext();

export const useAuth = () => useContext(AuthContext);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const loadStoredAuth = async () => {
      try {
        const storedUser = await SecureStore.getItemAsync('user');
        const storedToken = await SecureStore.getItemAsync('token');
        if (storedUser && storedToken) {
          setUser(JSON.parse(storedUser));
        }
      } catch (e) {
        showError('Authentication', 'Failed to load auth state');
      } finally {
        setIsLoading(false);
      }
    };
    loadStoredAuth();
  }, []);

  const login = async (email, password) => {
    try {
      const data = await loginUser(email, password);
      
      const token = data.accessToken || data.token;
      const userObj = {
        id: data.userId,
        name: typeof data.user === 'string' ? data.user : [data.user?.firstName, data.user?.lastName].filter(Boolean).join(' '),
        role: data.role,
        ...(typeof data.user === 'object' ? data.user : {})
      };

      await SecureStore.setItemAsync('token', token);
      await SecureStore.setItemAsync('user', JSON.stringify(userObj));
      setUser(userObj);
    } catch (error) {
      throw error;
    }
  };

  const register = async (firstName, lastName, contactNumber, email, password, role, licenseNumber) => {
    try {
      const userData = { firstName, lastName, contactNumber, email, password, role };
      
      // Only include license number if the role is BUS_DRIVER
      if (role === 'BUS_DRIVER') {
        userData.licenseNumber = licenseNumber;
      }

      const data = await registerUser(userData);
      return data;
    } catch (error) {
      throw error;
    }
  };

  const logout = async () => {
    await SecureStore.deleteItemAsync('token');
    await SecureStore.deleteItemAsync('user');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, isLoading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
};
