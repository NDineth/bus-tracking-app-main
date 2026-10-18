import API from './api';

export async function getBusTurns(busId) {
  const response = await API.get('/tracking/trips/history', { params: { busId } });
  if (!Array.isArray(response.data)) throw new Error('The server returned an unexpected trip history.');
  return response.data;
}

export async function getTurnReadings(assignmentId) {
  // This endpoint returns the entire assignment, with no recent-log limit.
  const response = await API.get(`/tracking/schedule-assignment/${assignmentId}/gps-logs`);
  if (!Array.isArray(response.data)) throw new Error('The server returned unexpected turn readings.');
  return response.data;
}
