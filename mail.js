import { readFileSync } from 'node:fs';
import handlebars from 'handlebars';
import { convert } from 'html-to-text';
import nodemailer from 'nodemailer';
import { longDate, parseStart, shortDate, time } from './dates.js';

// Each milestone fires once per event, anywhere inside [from, to] days before the event.
// The window (rather than an exact day) is what lets a missed run catch up the next day;
// state.js guarantees it still only goes out once.
const MILESTONES = [
  { kind: 'announcement', template: 'mail_announcement', from: 26, to: 28 },
  { kind: 'invitation-7', template: 'mail_invitation', from: 5, to: 7 },
  { kind: 'invitation-2', template: 'mail_invitation', from: 1, to: 2 },
];

const templates = new Map();
const render = (name, data) => {
  if (!templates.has(name)) {
    const source = readFileSync(new URL(`./templates/${name}.hbs`, import.meta.url), 'utf-8');
    templates.set(name, handlebars.compile(source));
  }
  return templates.get(name)(data);
};

let transport;
const getTransport = (smtp) => {
  transport ??= nodemailer.createTransport(smtp);
  return transport;
};

/** Milestone due for this event today, or null. */
export const due = (ev, diff) => {
  if (ev.externalEvent) return null;
  const milestone = MILESTONES.find((m) => diff >= m.from && diff <= m.to);
  if (!milestone) return null;
  // The announcement doubles as the "registration is open" mail.
  if (milestone.kind === 'announcement' && ev.hideRegistration) return null;
  return milestone;
};

export const send = async (ev, milestone, config) => {
  const at = parseStart(ev.start);
  const html = render(milestone.template, { ...ev, date: longDate(at), time: time(at) });
  const prefix = milestone.kind === 'announcement' ? 'Ankündigung für ' : '';

  const info = await getTransport(config.smtp).sendMail({
    from: config.from,
    to: config.to,
    subject: `${prefix}${shortDate(at)}: ${ev.summary}`,
    html,
    // `strip` used to just delete the tags, which dropped every link from the text part.
    text: convert(html, { wordwrap: 78, selectors: [{ selector: 'a', options: { hideLinkHrefIfSameAsText: true } }] }),
  });
  return info.messageId;
};
