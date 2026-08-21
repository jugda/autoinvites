// Proves the mail path end to end against a real SMTP server: the transport connects,
// authenticates with whatever SMTP_USER/SMTP_PASS were configured, and the message that
// arrives carries the right envelope, subject and both body parts.

import assert from 'node:assert/strict';
import { after, before, test } from 'node:test';
import { SMTPServer } from 'smtp-server';
import * as config from '../src/config.js';
import * as mail from '../src/mail.js';

const received = [];
let server;
let port;

const listen = (srv) => new Promise((resolve) => srv.listen(0, '127.0.0.1', () => resolve(srv.server.address().port)));

before(async () => {
  server = new SMTPServer({
    authOptional: false,
    disabledCommands: ['STARTTLS'],
    onAuth(auth, _session, callback) {
      if (auth.username === 'jugda' && auth.password === 's3cret') return callback(null, { user: auth.username });
      return callback(new Error('bad credentials'));
    },
    onData(stream, session, callback) {
      let raw = '';
      stream.on('data', (chunk) => { raw += chunk; });
      stream.on('end', () => {
        received.push({ envelope: session.envelope, raw });
        callback();
      });
    },
  });
  port = await listen(server);
});

after(() => server.close());

const env = (overrides = {}) => ({
  EVENTS_URL: 'https://example.invalid/events.json',
  MAIL_TO: 'liste@jug-da.de',
  MAIL_FROM: 'orga@jug-da.de',
  SMTP_HOST: '127.0.0.1',
  SMTP_PORT: String(port),
  SMTP_USER: 'jugda',
  SMTP_PASS: 's3cret',
  ...overrides,
});

const event = {
  uid: '20260917@jug-da.de',
  start: '2026-09-17T18:30:00',
  summary: 'Supercharging Java with Quarkus (Marco Klaassen)',
  location: 'MaibornWolff GmbH, Darmstadt',
  url: 'https://www.jug-da.de/2026/09/Quarkus-LLMs/',
};

test('delivers a real message through a plain authenticated SMTP server', async () => {
  const cfg = config.load(env());
  const milestone = mail.due(event, 7);

  const messageId = await mail.send(event, milestone, cfg.mail);
  assert.ok(messageId, 'expected a message id back from the server');

  assert.equal(received.length, 1);
  const { envelope, raw } = received[0];
  assert.equal(envelope.mailFrom.address, 'orga@jug-da.de');
  assert.deepEqual(envelope.rcptTo.map((r) => r.address), ['liste@jug-da.de']);

  // Subject is MIME-encoded because of the umlaut, so assert on the decoded form.
  const subject = raw.match(/^Subject: (.*)$/m)[1];
  const decoded = Buffer.from(subject.match(/\?B\?(.+)\?=/)?.[1] ?? '', 'base64').toString('utf-8');
  assert.equal(decoded || subject, '17.09.2026: Supercharging Java with Quarkus (Marco Klaassen)');

  assert.match(raw, /multipart\/alternative/);
  assert.match(raw, /text\/plain/);
  assert.match(raw, /text\/html/);
});

test('rejects a wrong SMTP password rather than silently dropping the mail', async () => {
  // A fresh module registry, because mail.js memoises its transport.
  const fresh = await import(`../src/mail.js?wrong-pass`);
  const cfg = config.load(env({ SMTP_PASS: 'wrong' }));
  await assert.rejects(() => fresh.send(event, fresh.due(event, 7), cfg.mail), /Invalid login|bad credentials/i);
});
