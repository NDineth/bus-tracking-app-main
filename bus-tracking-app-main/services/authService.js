import API from './api';

export const loginUser = async (email, password) => {
  try {
    const response = await API.post('/auth/sign-in', { email, password });
    return response.data;
  } catch (error) {
    throw error.response?.data?.message || 'Login failed';
  }
};

export const registerUser = async (userData) => {
  try {
    const response = await API.post('/auth/signup', userData);
    return response.data;
  } catch (error) {
    throw error.response?.data?.message || 'Registration failed';
  }
};

export default {
  loginUser,
  registerUser,
};
