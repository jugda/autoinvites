import { readFileSync } from 'node:fs';
import handlebars from 'handlebars';
import { createRestAPIClient } from 'masto';
import { dayMonth, parseStart } from './dates.js';

// Same catch-up windows as the mails, plus a post on the day itself. `same-day` stays an
// exact match on purpose: it renders "!!!HEUTE!!!", so it must not fire a day late.
const MILESTONES = [
  { kind: 'toot-28', from: 26, to: 28 },
  { kind: 'toot-7', from: 5, to: 7 },
  { kind: 'toot-2', from: 1, to: 2 },
  { kind: 'toot-0', from: 0, to: 0 },
];

let template;
const render = (data) => {
  if (!template) {
    const source = readFileSync(new URL('../templates/mastodon_invitation.hbs', import.meta.url), 'utf-8');
    template = handlebars.compile(source);
  }
  return template(data);
};

let client;
const getClient = (mastodon) => {
  client ??= createRestAPIClient(mastodon);
  return client;
};

export const due = (ev, diff) =>
  MILESTONES.find((m) => diff >= m.from && diff <= m.to) ?? null;

export const compose = (ev, diff) =>
  render({ ...ev, day: diff === 0 ? '!!!HEUTE!!! ' : dayMonth(parseStart(ev.start)) });

export const send = async (ev, diff, mastodon) => {
  const status = await getClient(mastodon).v1.statuses.create({
    status: compose(ev, diff),
    visibility: 'public',
    language: 'de',
  });
  return status.url;
};
