import { test } from 'node:test';
import assert from 'node:assert/strict';
import { classifyBraking, getBrakingDetails } from '../utils/braking.js';

test('braking X values use the supplied five severity bands', () => {
  for (const [x, severity] of [[0, 'Safe'], [-1.9, 'Safe'], [-2, 'Moderate'], [-3.49, 'Moderate'],
    [-3.5, 'Harsh'], [-4.99, 'Harsh'], [-5, 'Severe'], [-7, 'Severe'], [-7.01, 'Critical'], [-12, 'Critical']])
    assert.equal(classifyBraking(x)?.label, severity, `X=${x}`);
});
test('the severity messages match the requested braking guidance', () => {
  assert.equal(classifyBraking(-1).message, 'Smooth braking detected. Keep driving safely.');
  assert.equal(classifyBraking(-2.5).message, 'Sudden braking detected. Maintain a safe following distance.');
  assert.equal(classifyBraking(-4).message, 'Harsh braking detected! Brake gradually and anticipate traffic conditions.');
  assert.equal(classifyBraking(-6).message, 'Severe braking detected! Avoid sudden stops and maintain better control.');
  assert.equal(classifyBraking(-8).message, 'Extreme braking detected! Check your surroundings and ensure your vehicle is under control.');
});
test('missing, invalid and positive readings do not invent braking severity', () => {
  for (const x of [undefined, null, '', ' ', true, NaN, Infinity, 'not a reading', 3]) assert.equal(classifyBraking(x), null);
});
test('notifications with a raw X show severity text without numbers in their preview', () => {
  const details = getBrakingDetails({ title: 'Driving Safety Alert', brakingValueX: -4.25,
    body: 'Harsh Braking: 11\nSafety Score: 45 / 100' });
  assert.equal(details.level.label, 'Harsh');
  assert.doesNotMatch(details.message, /\d/);
  assert.equal(details.x, -4.25);
  assert.equal(details.count, 11);
  assert.doesNotMatch(details.detailsBody, /Harsh Braking: 11/);
  assert.match(details.detailsBody, /Safety Score: 45/);
});
test('explicit braking readings in notification text and sensor fields are supported', () => {
  assert.equal(getBrakingDetails({ title: 'Braking alert', body: 'Braking value (X): -6.5' }).level.label, 'Severe');
  assert.equal(getBrakingDetails({ title: 'SUDDEN_BRAKING', body: 'X: -2.25' }).level.label, 'Moderate');
  assert.equal(getBrakingDetails({ title: 'Braking alert', acceleration: { x: -7.25 } }).level.label, 'Critical');
  assert.equal(getBrakingDetails({ title: 'Braking alert', accelerationX: '0' }).level.label, 'Safe');
  assert.equal(getBrakingDetails({ title: 'Braking alert', body: 'X: -5e-1' }).level.label, 'Safe');
});
test('legacy braking counts are retained for details and are never treated as X', () => {
  for (const count of [0, 2, 7, 12]) {
    const details = getBrakingDetails({ title: 'Driving Safety Alert: Bus QA-1001', body: `Harsh Braking: ${count}\nAggressive Acceleration: 3` });
    assert.equal(details.count, count);
    assert.equal(details.x, null);
    assert.equal(details.level, null);
    assert.doesNotMatch(details.message, /\d/);
  }
});
test('unrelated notifications are unchanged even if they contain another X value', () => {
  assert.equal(getBrakingDetails({ title: 'Approval request updated', body: 'Your request was approved.' }), null);
  assert.equal(getBrakingDetails({ title: 'GPS update', x: -4 }), null);
});
