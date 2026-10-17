export const BRAKING_LEVELS = Object.freeze({
  SAFE: { label: 'Safe', color: '#176B60', background: '#E9F4F0',
    message: 'Smooth braking detected. Keep driving safely.' },
  MODERATE: { label: 'Moderate', color: '#94610A', background: '#FFF7E5',
    message: 'Sudden braking detected. Maintain a safe following distance.' },
  HARSH: { label: 'Harsh', color: '#B45123', background: '#FFF0E7',
    message: 'Harsh braking detected! Brake gradually and anticipate traffic conditions.' },
  SEVERE: { label: 'Severe', color: '#C13C2D', background: '#FCEEEB',
    message: 'Severe braking detected! Avoid sudden stops and maintain better control.' },
  CRITICAL: { label: 'Critical', color: '#BD3347', background: '#FCECEF',
    message: 'Extreme braking detected! Check your surroundings and ensure your vehicle is under control.' },
});

const numberValue = value => {
  if ((typeof value !== 'number' && typeof value !== 'string') || String(value).trim() === '') return null;
  const numeric = Number(value);
  return Number.isFinite(numeric) ? numeric : null;
};

export function classifyBraking(value) {
  const x = numberValue(value);
  if (x === null || x > 0) return null;
  // Shared boundaries enter the stronger band; "below -7" is strictly less than -7.
  if (x < -7) return BRAKING_LEVELS.CRITICAL;
  if (x <= -5) return BRAKING_LEVELS.SEVERE;
  if (x <= -3.5) return BRAKING_LEVELS.HARSH;
  if (x <= -2) return BRAKING_LEVELS.MODERATE;
  return BRAKING_LEVELS.SAFE;
}

const numericPattern = '([-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][-+]?\\d+)?)(?![\\d.eE])';
const brakingValuePattern = new RegExp(`\\bbraking\\s*(?:value|reading)\\s*(?:\\(\\s*x\\s*\\))?\\s*[:=]\\s*${numericPattern}`, 'i');
const axisPattern = new RegExp(`(?:^|[\\s,;])(?:acceleration[ _-]*)?x(?:\\s*(?:value|reading))?\\s*[:=]\\s*${numericPattern}`, 'i');
const countPattern = /\bharsh\s+braking(?:\s+count)?\s*[:=]\s*(\d+)(?![\d.])/i;
const countLinePattern = /^\s*harsh\s+braking(?:\s+count)?\s*[:=]\s*\d+\s*$/i;

export function getBrakingDetails(notification) {
  const body = typeof notification.body === 'string' ? notification.body : '';
  const rawBrakingValue = notification.brakingValueX ?? notification.brakingX;
  const bodyValue = body.match(brakingValuePattern)?.[1];
  const count = numberValue(notification.harshBrakingCount ?? body.match(countPattern)?.[1]);
  const isBraking = rawBrakingValue != null || bodyValue != null || count !== null
    || /braking/i.test(`${notification.title || ''} ${notification.behavior || ''}`)
    || /(?:sudden|harsh|severe|smooth|extreme)\s+braking\s+detected/i.test(body);
  if (!isBraking) return null;

  // A count is never used as acceleration X. Legacy notifications contain only counts.
  const values = [rawBrakingValue, notification.accelerationX, notification.acceleration?.x,
    notification.x, bodyValue, body.match(axisPattern)?.[1]];
  const x = values.map(numberValue).find(value => value !== null) ?? null;
  const level = classifyBraking(x);
  const message = level?.message || (x !== null
    ? 'This reading is outside the braking ranges. Open details to review the recorded value.'
    : count === 0 ? 'No harsh braking events were reported. Open details to review the event.'
      : 'Braking was reported. Open details to review the available event data.');
  return { x, count: Number.isInteger(count) && count >= 0 ? count : null, level, message,
    detailsBody: body.split(/\r?\n/).filter(line => !countLinePattern.test(line)).join('\n').trim() };
}
