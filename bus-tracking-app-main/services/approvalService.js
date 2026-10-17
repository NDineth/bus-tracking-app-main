import API from './api';

export const submitApprovalRequest = async (requestData) => {
  try {
    const response = await API.post('/approvals/request', requestData);
    return response.data;
  } catch (error) {
    throw error.response?.data?.message || 'Failed to submit approval request';
  }
};

export const getUserApprovalRequests = async (userId) => {
  try {
    const response = await API.get(`/approvals/user/${userId}`);
    return response.data;
  } catch (error) {
    throw error.response?.data?.message || 'Failed to fetch approval history';
  }
};

export default {
  submitApprovalRequest,
  getUserApprovalRequests,
};
