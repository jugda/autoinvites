import assert from 'node:assert/strict';
import { test } from 'node:test';
import * as config from '../src/config.js';

const base = {
  EVENTS_URL: 'https://example.invalid/events.json',
  MAIL_TO: 'liste@jug-da.de',
  MAIL_FROM: 'orga@jug-da.de',
  SMTP_HOST: 'smtp.example.invalid',
  SMTP_USER: 'user',
  SMTP_PASS: 'pass',
};

test('reports every missing variable at once, not just the first', () => {
  assert.throws(
    () => config.load({ SMTP_HOST: 'smtp.example.invalid' }),
    /EVENTS_URL, MAIL_TO, MAIL_FROM/,
  );
});

test('defaults to submission on 587 with STARTTLS', () => {
  const { smtp } = config.load(base).mail;
  assert.equal(smtp.port, 587);
  assert.equal(smtp.secure, false);
  assert.deepEqual(smtp.auth, { user: 'user', pass: 'pass' });
});

test('treats 465 as implicit TLS', () => {
  assert.equal(config.load({ ...base, SMTP_PORT: '465' }).mail.smtp.secure, true);
});

test('SMTP_SECURE overrides the port convention in both directions', () => {
  assert.equal(config.load({ ...base, SMTP_PORT: '587', SMTP_SECURE: 'true' }).mail.smtp.secure, true);
  assert.equal(config.load({ ...base, SMTP_PORT: '465', SMTP_SECURE: 'false' }).mail.smtp.secure, false);
});

test('rejects a nonsense port instead of silently using NaN', () => {
  assert.throws(() => config.load({ ...base, SMTP_PORT: 'submission' }), /SMTP_PORT must be a port number/);
});

test('allows a relay that authorises by IP instead of credentials', () => {
  const { SMTP_USER, SMTP_PASS, ...noAuth } = base;
  assert.equal(config.load(noAuth).mail.smtp.auth, undefined);
});

test('a username without a password is a misconfiguration, not anonymous auth', () => {
  const { SMTP_PASS, ...partial } = base;
  assert.throws(() => config.load(partial), /SMTP_PASS/);
});

test('Mastodon is optional but must be configured in full', () => {
  assert.equal(config.load(base).mastodon, null);
  assert.throws(() => config.load({ ...base, MASTO_URL: 'https://m.example' }), /MASTO_TOKEN/);
  assert.throws(() => config.load({ ...base, MASTO_TOKEN: 't' }), /MASTO_URL/);
  assert.deepEqual(config.load({ ...base, MASTO_URL: 'https://m.example', MASTO_TOKEN: 't' }).mastodon,
    { url: 'https://m.example', accessToken: 't' });
});

test('DRY_RUN accepts the usual truthy spellings and defaults off', () => {
  assert.equal(config.load(base).dryRun, false);
  for (const value of ['true', 'TRUE', '1', 'yes', 'on']) {
    assert.equal(config.load({ ...base, DRY_RUN: value }).dryRun, true, `expected ${value} to enable dry run`);
  }
  for (const value of ['false', '0', 'no', '']) {
    assert.equal(config.load({ ...base, DRY_RUN: value }).dryRun, false, `expected ${value} to disable dry run`);
  }
});

test('an unset Actions variable arrives as an empty string, not undefined', () => {
  // GitHub renders ${{ vars.MISSING }} as '', which must fall back to the defaults.
  const cfg = config.load({ ...base, SMTP_PORT: '', SMTP_SECURE: '', DRY_RUN: '', MASTO_URL: '', MASTO_TOKEN: '' });
  assert.equal(cfg.mail.smtp.port, 587);
  assert.equal(cfg.mail.smtp.secure, false);
  assert.equal(cfg.dryRun, false);
  assert.equal(cfg.mastodon, null);
});
