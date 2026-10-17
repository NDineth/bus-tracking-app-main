import { Alert, Platform } from 'react-native';

export const showAlert = (title, message, buttons) => {
  if (Platform.OS === 'web') {
    window.alert(`${title}\n\n${message}`);
    buttons?.find(button => button.style !== 'cancel')?.onPress?.();
  } else Alert.alert(title, message, buttons);
};

/**
 * Shows an error alert to the user and logs the error to the console.
 * @param {string} title - The title of the alert.
 * @param {any} error - The error object or message.
 */
export const showError = (title, error) => {
  const message = error?.message || (typeof error === 'string' ? error : 'An unexpected error occurred');
  
  // Log to console for development (using log instead of error as requested to remove console.error)
  console.log(`[ERROR] ${title}:`, error);
  
  showAlert(
    title,
    message,
    [{ text: 'OK' }]
  );
};

export default {
  showError,
};
