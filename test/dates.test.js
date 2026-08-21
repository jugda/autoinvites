import assert from 'node:assert/strict';
import { test } from 'node:test';
import { dayMonth, daysUntil, longDate, parseStart, shortDate, time } from '../dates.js';

// These are the exact strings moment (locale 'de') produced before it was dropped.
test('formats German dates the way moment did', () => {
  const at = parseStart('2026-08-20T18:30:00');
  assert.equal(shortDate(at), '20.08.2026');
  assert.equal(longDate(at), 'Donnerstag, 20. August 2026');
  assert.equal(time(at), '18:30');
  assert.equal(dayMonth(at), '20.08.');
});

test('renders the feed wall-clock regardless of the runner timezone', () => {
  const original = process.env.TZ;
  try {
    for (const tz of ['UTC', 'Europe/Berlin', 'Pacific/Kiritimati', 'America/Los_Angeles']) {
      process.env.TZ = tz;
      assert.equal(time(parseStart('2026-08-20T18:30:00')), '18:30', `wrong under TZ=${tz}`);
    }
  } finally {
    process.env.TZ = original;
  }
});

test('rejects timestamps it cannot read as a naive wall-clock', () => {
  assert.equal(parseStart(undefined), null);
  assert.equal(parseStart(''), null);
  assert.equal(daysUntil(undefined), null);
});

test('counts whole days to the event, in Berlin', () => {
  const now = new Date('2026-08-01T09:00:00Z');
  assert.equal(daysUntil('2026-08-01T18:30:00', now), 0);
  assert.equal(daysUntil('2026-08-03T18:30:00', now), 2);
  assert.equal(daysUntil('2026-08-29T18:30:00', now), 28);
  assert.equal(daysUntil('2026-07-31T18:30:00', now), -1);
});

test('is not thrown off by the DST switch', () => {
  // Germany leaves CEST on 2026-10-25; a naive day count would be off by one here.
  const now = new Date('2026-10-23T09:00:00Z');
  assert.equal(daysUntil('2026-10-30T19:00:00', now), 7);
});

test('uses Berlin, not UTC, to decide what day it is', () => {
  // 23:30 UTC on the 1st is already 01:30 on the 2nd in Berlin (CEST).
  const now = new Date('2026-08-01T23:30:00Z');
  assert.equal(daysUntil('2026-08-02T18:30:00', now), 0);
});
