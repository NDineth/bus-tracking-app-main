import API from './api';
export const getAlertPolicy = async () => (await API.get('/alert-policy')).data;
export const saveAlertPolicy = async (policy) => (await API.put('/alert-policy', policy)).data;
