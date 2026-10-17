import { test } from 'node:test';
import assert from 'node:assert/strict';
import { requestErrorMessage, resolveApiBaseUrl } from '../utils/connection.js';

const backend = 'http://172.20.10.2:8080/api';
test('a remote backend keeps its own host rather than the frontend host', () => {
  assert.equal(resolveApiBaseUrl(undefined, backend), backend);
  assert.equal(resolveApiBaseUrl('', backend), backend);
  assert.equal(resolveApiBaseUrl(' https://example.test/api/// ', backend), 'https://example.test/api');
});
test('network and timeout errors identify the actual backend address', () => {
  assert.match(requestErrorMessage({ isAxiosError: true, message: 'Network Error' }, backend), /Cannot connect to http:\/\/172\.20\.10\.2:8080\/api/);
  assert.match(requestErrorMessage({ code: 'ECONNABORTED' }, backend), /did not respond in time/);
});
test('authorization and missing endpoints are not reported as missing assignments', () => {
  assert.match(requestErrorMessage({ response: { status: 401 } }, backend), /sign in again/);
  assert.match(requestErrorMessage({ response: { status: 403 } }, backend), /permission/);
  assert.match(requestErrorMessage({ response: { status: 404 } }, backend), /service was not found/);
});
test('registration validation and duplicate email messages are preserved', () => {
  assert.match(requestErrorMessage({ config: { url: '/auth/sign-in' }, response: { status: 401 } }, backend), /email address or password is incorrect/);
  assert.equal(requestErrorMessage({ response: { status: 400, data: { message: 'Email Address already in use!' } } }, backend), 'Email Address already in use!');
  assert.equal(requestErrorMessage({ response: { status: 400, data: { errors: { email: 'Invalid email', password: 'Password required' } } } }, backend), 'Invalid email. Password required');
});
