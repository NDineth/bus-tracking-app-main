export function resolveApiBaseUrl(configuredUrl, fallbackUrl) {
  const explicitUrl = configuredUrl?.trim();
  return (explicitUrl || fallbackUrl).replace(/\/+$/, '');
}

export function requestErrorMessage(error, apiBaseUrl) {
  if (typeof error === 'string') return error;
  const status = error?.response?.status;
  const body = error?.response?.data;
  const validation = body?.errors && Object.values(body.errors).filter(value => typeof value === 'string').join('. ');
  if (status === 401) return error?.config?.url === '/auth/sign-in'
    ? 'The email address or password is incorrect. Please try again.'
    : 'Your sign-in session is missing or has expired. Please sign out and sign in again.';
  if (status === 403) return 'Your account does not have permission to access this information.';
  if (status === 404) return `The requested service was not found at ${apiBaseUrl}. Check the backend API address.`;
  if (typeof body?.message === 'string' && body.message) return body.message;
  if (validation) return validation;
  if (['ECONNABORTED', 'ETIMEDOUT'].includes(error?.code)) return `The server at ${apiBaseUrl} did not respond in time. Check that the backend is running and reachable from this device.`;
  if (error?.isAxiosError && !error.response) return `Cannot connect to ${apiBaseUrl}. Check the backend address and make sure this device can reach the server.`;
  return error?.message || 'Unable to complete the request. Please try again.';
}
