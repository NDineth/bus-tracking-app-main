import { safetyScore } from './alertPolicy.js';

const numeric = value => {
  if (!['number', 'string'].includes(typeof value) || String(value).trim() === '') return null;
  const result = Number(value);
  return Number.isFinite(result) && result >= 0 ? result : null;
};

export function drivingAlertDetails(notification) {
  const body = typeof notification.body === 'string' ? notification.body : '';
  const count = (fields, pattern) => {
    const raw = fields.map(field => notification[field]).find(value => value != null);
    const value = numeric(raw ?? body.match(pattern)?.[1]);
    return Number.isInteger(value) ? value : null;
  };
  const rating = notification.driverRating ?? body.match(/Driver Rating:\s*([^\r\n]+)/i)?.[1];
  return {
    highSpeed: count(['highSpeedCount', 'highSpeedEvents'], /(?:High Speed|Overspeeding)(?: Events| Count)?\s*[:=]\s*(\d+(?:\.\d+)?)/i),
    braking: count(['harshBrakingCount', 'suddenBrakingCount'], /(?:Harsh|Sudden) Braking(?: Count)?\s*[:=]\s*(\d+(?:\.\d+)?)/i),
    acceleration: count(['dangerousAccelerationCount', 'aggressiveAccelerationCount', 'suddenAccelerationCount'], /(?:Dangerous|Aggressive|Sudden) Acceleration(?: Events| Count)?\s*[:=]\s*(\d+(?:\.\d+)?)/i),
    turning: count(['dangerousTurnCount'], /Dangerous Turns?(?: Count)?\s*[:=]\s*(\d+(?:\.\d+)?)/i),
    highSpeedDetected: /HIGH[ _-]SPEED/i.test(`${notification.behavior || ''} ${notification.title || ''}`),
    score: safetyScore(notification),
    rating: typeof rating === 'string' && rating.trim() ? rating.trim().replace(/_/g, ' ') : null,
  };
}

export function sortTurns(turns, busId) {
  const rank = status => ['ACTIVE', 'IN_PROGRESS'].includes(status) ? 0 : 1;
  const dailyCounts = new Map();
  return turns.filter(turn => String(turn.busId) === String(busId) && !['CANCELLED', 'TURN_LOSS'].includes(turn.status))
    .sort((a, b) => String(a.startTime || '').localeCompare(String(b.startTime || '')) || String(a.id).localeCompare(String(b.id)))
    .map(turn => {
      const day = String(turn.startTime || '').slice(0, 10);
      const turnNumber = (dailyCounts.get(day) || 0) + 1;
      dailyCounts.set(day, turnNumber);
      return { ...turn, turnNumber };
    })
    .sort((a, b) => rank(a.status) - rank(b.status) || String(b.startTime || '').localeCompare(String(a.startTime || '')));
}

export function turnLabel(turn) {
  const date = new Date(turn.startTime);
  const time = Number.isNaN(date.getTime()) ? 'Time unavailable' : date.toLocaleString('en-LK', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
  return `Turn ${turn.turnNumber || turn.id} · ${time}`;
}

// Keep every reading, including nulls (missing data is not a zero).
export function turnReadings(logs, assignmentId, busId) {
  return logs.filter(log => log && String(log.scheduleAssignmentId) === String(assignmentId)
      && (busId == null || String(log.busId) === String(busId)) && Number.isFinite(Date.parse(log.recordedAt)))
    .sort((a, b) => String(a.recordedAt || '').localeCompare(String(b.recordedAt || '')))
    .map(log => ({ ...log, speed: numeric(log.speed), suddenBrakingCount: numeric(log.suddenBrakingCount),
      dangerousAccelerationCount: numeric(log.dangerousAccelerationCount ?? log.aggressiveAccelerationCount),
      dangerousTurnCount: numeric(log.dangerousTurnCount), safetyScore: numeric(log.safetyScore) }));
}

export function graphBuckets(readings, field, aggregate = 'max') {
  if (!readings.length) return [];
  const start = Date.parse(readings[0].recordedAt), end = Date.parse(readings[readings.length - 1].recordedAt);
  const size = Math.min(32, readings.length);
  const groups = Array.from({ length: size }, () => []);
  readings.forEach((reading, index) => {
    const fraction = end > start ? (Date.parse(reading.recordedAt) - start) / (end - start) : index / Math.max(1, readings.length - 1);
    groups[Math.min(size - 1, Math.max(0, Math.floor(fraction * size)))].push(reading);
  });
  return groups.map((group, index) => {
    const values = group.map(reading => reading[field]).filter(value => value !== null && value !== undefined);
    // A partially missing interval must not be reported as a complete count/score.
    const complete = group.length > 0 && values.length === group.length;
    const value = !complete ? null : aggregate === 'sum' ? values.reduce((a, b) => a + b, 0)
    : aggregate === 'min' ? Math.min(...values) : Math.max(...values);
    return { value, time: group[0]?.recordedAt || new Date(start + (end - start) * index / size).toISOString() };
  });
}
