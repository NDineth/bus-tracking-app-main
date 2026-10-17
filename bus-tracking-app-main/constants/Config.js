import { resolveApiBaseUrl } from '../utils/connection';

const IP_ADDRESS = '172.20.10.2';
const API_BASE_URL = resolveApiBaseUrl(process.env.EXPO_PUBLIC_API_URL, `http://${IP_ADDRESS}:8080/api`);

export default {
  API_BASE_URL,
  IP_ADDRESS,
};
