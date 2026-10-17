const IP_ADDRESS = '192.168.1.56';
const PORT = '8080';

export default {
  API_BASE_URL: process.env.EXPO_PUBLIC_API_URL || `http://${IP_ADDRESS}:${PORT}/api`,
  IP_ADDRESS,
};
