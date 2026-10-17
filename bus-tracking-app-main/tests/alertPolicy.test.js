import { test } from 'node:test';
import assert from 'node:assert/strict';
import { DEFAULT_POLICY, scoreSeverity, safetyScore, alertSeverity, validatePolicy, notificationId, isUnread, sortNotifications } from '../utils/alertPolicy.js';

test('score boundaries put equality in the safer band', () => {
  for (const [score, expected] of [[0, 'CRITICAL'], [39, 'CRITICAL'], [40, 'HIGH'], [69, 'HIGH'], [70, 'CAUTION'], [89, 'CAUTION'], [90, 'LOW'], [100, 'LOW']])
    assert.equal(scoreSeverity(score), expected);
});
test('custom thresholds affect classification', () => {
  const policy = { ...DEFAULT_POLICY, criticalBelow: 20, highRiskBelow: 50, cautionBelow: 80 };
  assert.equal(scoreSeverity(40, policy), 'HIGH');
  assert.equal(scoreSeverity(80, policy), 'LOW');
});
test('legacy notifications parse scores without inventing missing scores', () => {
  assert.equal(safetyScore({ body: 'Safety Score: 65 / 100\nDriver Rating: High Risk' }), 65);
  assert.equal(safetyScore({ body: 'Safety Score: 0 / 100' }), 0);
  assert.equal(safetyScore({ safetyScore: 101 }), null);
  assert.equal(safetyScore({ body: 'Schedule updated' }), null);
  assert.equal(alertSeverity({ title: 'Driving Alert: CRASH DETECTED' }), 'CRITICAL');
  assert.equal(alertSeverity({ title: 'Approval request updated' }), 'INFO');
  assert.equal(alertSeverity({ safetyScore: 65 }), 'HIGH');
});
test('server-recorded severity survives later policy changes', () => {
  assert.equal(alertSeverity({ safetyScore: 65, severity: 'CAUTION' }), 'CAUTION');
});
test('policy validation rejects malformed and unordered thresholds', () => {
  assert.equal(validatePolicy(DEFAULT_POLICY), null);
  for (const patch of [{ criticalBelow: 70 }, { highRiskBelow: 90 }, { cautionBelow: 101 }, { criticalBelow: 0 },
    { highSpeedEvents: 0 }, { brakingEvents: 101 }, { accelerationEvents: '2.5' }, { turningEvents: '' }, { cautionBelow: 'NaN' }])
    assert.ok(validatePolicy({ ...DEFAULT_POLICY, ...patch }), JSON.stringify(patch));
  assert.equal(validatePolicy({ ...DEFAULT_POLICY, criticalBelow: '40' }), null);
});
test('notification operations use notification ID rather than recipient ID', () => {
  assert.equal(notificationId({ id: 98, notificationId: 7 }), 7);
  assert.equal(notificationId({ id: 8 }), 8);
  assert.equal(isUnread({ isRead: true }), false);
  assert.equal(isUnread({ read: true }), false);
  assert.equal(isUnread({ isRead: false, read: true }), true);
});
test('newest-first sorting is stable and does not mutate the API data', () => {
  const items = [{ id: 1, createdAt: '2026-10-16T11:00:00' }, { id: 2, createdAt: '2026-10-17T11:00:00' }];
  assert.equal(sortNotifications(items)[0].id, 2);
  assert.equal(items[0].id, 1);
});
