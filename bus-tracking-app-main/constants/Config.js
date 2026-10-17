const IP_ADDRESS = '172.20.10.2';
const PORT = '8080';

export default {
  API_BASE_URL: process.env.EXPO_PUBLIC_API_URL || `http://${IP_ADDRESS}:${PORT}/api`,
  IP_ADDRESS,
};
