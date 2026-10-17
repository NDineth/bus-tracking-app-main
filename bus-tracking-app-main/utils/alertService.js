import { Alert } from 'react-native';

/**
 * Shows an error alert to the user and logs the error to the console.
 * @param {string} title - The title of the alert.
 * @param {any} error - The error object or message.
 */
export const showError = (title, error) => {
  const message = error?.message || (typeof error === 'string' ? error : 'An unexpected error occurred');
  
  // Log to console for development (using log instead of error as requested to remove console.error)
  console.log(`[ERROR] ${title}:`, error);
  
  Alert.alert(
    title,
    message,
    [{ text: 'OK' }]
  );
};

export default {
  showError,
};
