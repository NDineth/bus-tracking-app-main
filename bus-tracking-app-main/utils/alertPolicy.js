// Score bands have a display fallback; driving event counts must come from the server.
export const DEFAULT_POLICY = Object.freeze({ criticalBelow: 40, highRiskBelow: 70, cautionBelow: 90 });
export const EVENT_POLICY_FIELDS = Object.freeze(['highSpeedEvents', 'brakingEvents', 'accelerationEvents', 'turningEvents']);
export const POLICY_FIELDS = Object.freeze([...Object.keys(DEFAULT_POLICY), ...EVENT_POLICY_FIELDS]);
export const createPolicyDraft = (policy = {}) => Object.fromEntries(POLICY_FIELDS.map(key => [key, policy[key] ?? '']));
export const policyHasChanges = (draft, policy) => POLICY_FIELDS.some(key => String(draft[key] ?? '') !== String(policy[key] ?? ''));

export const SEVERITIES = {
  CRITICAL: { label: 'Critical', color: '#BD3347', background: '#FCECEF', icon: 'alert-circle-outline' },
  HIGH: { label: 'High risk', color: '#B45123', background: '#FFF0E7', icon: 'warning-outline' },
  CAUTION: { label: 'Caution', color: '#94610A', background: '#FFF7E5', icon: 'shield-half-outline' },
  LOW: { label: 'Low risk', color: '#176B60', background: '#E9F4F0', icon: 'shield-checkmark-outline' },
  INFO: { label: 'Update', color: '#476883', background: '#EDF3F8', icon: 'notifications-outline' },
};
export const notificationId = (notification) => notification.notificationId ?? notification.id;
export const isUnread = (notification) => !(notification.isRead ?? notification.read ?? false);
export function alertType(notification) {
  const title = typeof notification.title === 'string' ? notification.title.trim() : '';
  if (/^Driving Safety Alert:\s*Bus\b/i.test(title)) return 'Driving Safety Alert';
  if (/^Driving Alert:\s*/i.test(title)) return title.replace(/^Driving Alert:\s*/i, '').replace(/_/g, ' ') || 'Driving Alert';
  if (/^Schedule Adjustment:/i.test(title)) return 'Schedule Adjustment';
  if (/^(?:New )?Approval Request\b/i.test(title)) return 'Approval Request';
  return title || 'Notification';
}
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
export function validateScorePolicy(policy) {
  for (const key of Object.keys(DEFAULT_POLICY)) {
    if (policy[key] == null || String(policy[key]).trim() === '' || !Number.isInteger(Number(policy[key]))) return 'Enter a whole number for every score threshold.';
  }
  const { criticalBelow: c, highRiskBelow: h, cautionBelow: w } = Object.fromEntries(
    Object.entries(policy).map(([key, value]) => [key, Number(value)]));
  if (!(0 < c && c < h && h < w && w <= 100)) return 'Score thresholds must increase: 0 < critical < high risk < caution <= 100.';
  return null;
}
export function validatePolicy(policy) {
  const scoreError = validateScorePolicy(policy);
  if (scoreError) return scoreError;
  if (EVENT_POLICY_FIELDS.some(key => policy[key] == null || String(policy[key]).trim() === '' || !Number.isInteger(Number(policy[key]))))
    return 'Enter a whole number for every driving event threshold.';
  if (EVENT_POLICY_FIELDS.some(key => Number(policy[key]) < 1 || Number(policy[key]) > 100))
    return 'Event thresholds must be between 1 and 100.';
  return null;
}
export const sortNotifications = (items) => [...items].sort((a, b) =>
  (Date.parse(b.createdAt) || 0) - (Date.parse(a.createdAt) || 0));
