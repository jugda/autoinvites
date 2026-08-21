import assert from 'node:assert/strict';
import { test } from 'node:test';
import * as mail from '../mail.js';
import * as toot from '../toot.js';
import * as state from '../state.js';

const event = (extra = {}) => ({ uid: 'e1', start: '2026-08-20T18:30:00', summary: 's', ...extra });

test('mail milestones cover the old exact-day offsets', () => {
  assert.equal(mail.due(event(), 28).kind, 'announcement');
  assert.equal(mail.due(event(), 7).kind, 'invitation-7');
  assert.equal(mail.due(event(), 2).kind, 'invitation-2');
});

test('mail milestones catch up for a day or two after a missed run', () => {
  assert.equal(mail.due(event(), 26).kind, 'announcement');
  assert.equal(mail.due(event(), 5).kind, 'invitation-7');
  assert.equal(mail.due(event(), 1).kind, 'invitation-2');
});

test('mail milestones do not overlap or fire outside their window', () => {
  for (const diff of [40, 29, 25, 8, 4, 3, 0, -1]) {
    assert.equal(mail.due(event(), diff), null, `unexpected mail at ${diff}d`);
  }
});

test('external events never get a mail', () => {
  for (const diff of [28, 7, 2]) {
    assert.equal(mail.due(event({ externalEvent: true }), diff), null);
  }
});

test('hideRegistration suppresses only the announcement', () => {
  const ev = event({ hideRegistration: true });
  assert.equal(mail.due(ev, 28), null);
  assert.equal(mail.due(ev, 7).kind, 'invitation-7');
});

test('toots also go out on the day itself, but only exactly then', () => {
  assert.equal(toot.due(event(), 0).kind, 'toot-0');
  assert.equal(toot.due(event(), -1), null);
});

test('toots go to external events too', () => {
  assert.equal(toot.due(event({ externalEvent: true }), 7).kind, 'toot-7');
});

test('the same-day toot says HEUTE, the others carry the date', () => {
  assert.match(toot.compose(event(), 0), /^!!!HEUTE!!! /);
  assert.match(toot.compose(event({ title: 't' }), 7), /^20\.08\.: t/);
});

test('pruning keeps anything still in the feed', () => {
  const sent = new Map([['e1:toot-7', '2020-01-01']]);
  state.prune(sent, new Set(['e1']), new Date('2026-08-01'));
  assert.ok(sent.has('e1:toot-7'));
});

test('pruning keeps recent sends even once the event leaves the feed', () => {
  const sent = new Map([['gone:toot-7', '2026-07-20']]);
  state.prune(sent, new Set(), new Date('2026-08-01'));
  assert.ok(sent.has('gone:toot-7'));
});

test('pruning drops only old sends for events that are gone', () => {
  const sent = new Map([['gone:toot-7', '2026-01-01']]);
  state.prune(sent, new Set(), new Date('2026-08-01'));
  assert.equal(sent.size, 0);
});

test('an empty feed cannot wipe recent state', () => {
  const sent = new Map([['e1:toot-7', '2026-07-30'], ['e2:invitation-2', '2026-07-25']]);
  state.prune(sent, new Set(), new Date('2026-08-01'));
  assert.equal(sent.size, 2);
});
