export const DEFAULT_POLICY = Object.freeze({ criticalBelow: 40, highRiskBelow: 70,
  cautionBelow: 90, highSpeedEvents: 3, brakingEvents: 2, accelerationEvents: 2, turningEvents: 2 });

export const SEVERITIES = {
  CRITICAL: { label: 'Critical', color: '#BD3347', background: '#FCECEF', icon: 'alert-circle-outline' },
  HIGH: { label: 'High risk', color: '#B45123', background: '#FFF0E7', icon: 'warning-outline' },
  CAUTION: { label: 'Caution', color: '#94610A', background: '#FFF7E5', icon: 'shield-half-outline' },
  LOW: { label: 'Low risk', color: '#176B60', background: '#E9F4F0', icon: 'shield-checkmark-outline' },
  INFO: { label: 'Update', color: '#476883', background: '#EDF3F8', icon: 'notifications-outline' },
};
export const notificationId = (notification) => notification.notificationId ?? notification.id;
export const isUnread = (notification) => !(notification.isRead ?? notification.read ?? false);
export function safetyScore(notification) {
  const raw = notification.safetyScore ?? notification.body?.match(/Safety Score:\s*(\d+(?:\.\d+)?)\s*\/\s*100/i)?.[1];
  if (raw === undefined || raw === null || raw === '') return null;
  const score = Number(raw);
  return Number.isFinite(score) && score >= 0 && score <= 100 ? score : null;
}
export function scoreSeverity(score, policy = DEFAULT_POLICY) {
  if (score === null || !Number.isFinite(score)) return 'INFO';
  if (score < policy.criticalBelow) return 'CRITICAL';
  if (score < policy.highRiskBelow) return 'HIGH';
  if (score < policy.cautionBelow) return 'CAUTION';
  return 'LOW';
}
export function alertSeverity(notification, policy = DEFAULT_POLICY) {
  // Preserve the severity recorded when a server alert was generated.
  if (SEVERITIES[notification.severity]) return notification.severity;
  const text = `${notification.title || ''} ${notification.body || ''}`;
  if (/crash|\burgent\b/i.test(text)) return 'CRITICAL';
  const score = safetyScore(notification);
  if (score !== null) return scoreSeverity(score, policy);
  if (/driving alert|violation alert/i.test(text)) return 'HIGH';
  return 'INFO';
}
export function validatePolicy(policy) {
  for (const key of Object.keys(DEFAULT_POLICY)) {
    if (String(policy[key]).trim() === '' || !Number.isInteger(Number(policy[key]))) return 'Enter a whole number for every field.';
  }
  const { criticalBelow: c, highRiskBelow: h, cautionBelow: w } = Object.fromEntries(
    Object.entries(policy).map(([key, value]) => [key, Number(value)]));
  if (!(0 < c && c < h && h < w && w <= 100)) return 'Score thresholds must increase: 0 < critical < high risk < caution <= 100.';
  if (['highSpeedEvents', 'brakingEvents', 'accelerationEvents', 'turningEvents'].some(key => Number(policy[key]) < 1 || Number(policy[key]) > 100))
    return 'Event thresholds must be between 1 and 100.';
  return null;
}
export const sortNotifications = (items) => [...items].sort((a, b) =>
  (Date.parse(b.createdAt) || 0) - (Date.parse(a.createdAt) || 0));
