// One-off cutover helper.
//
// The old Lambda fired on exact day offsets (mail 28/7/2, toot 28/7/2/0) and kept no
// record. The new code uses catch-up windows, so starting from empty state would re-send
// anything already inside a window. This marks every milestone whose trigger day has
// already been reached as "sent by the old system", so the cutover is silent.
//
//   EVENTS_URL=https://www.jug-da.de/events.json node scripts/seed-state.js

import { daysUntil } from '../dates.js';
import * as state from '../state.js';

const OLD_TRIGGERS = [
  { kind: 'announcement', day: 28, mail: true },
  { kind: 'invitation-7', day: 7, mail: true },
  { kind: 'invitation-2', day: 2, mail: true },
  { kind: 'toot-28', day: 28 },
  { kind: 'toot-7', day: 7 },
  { kind: 'toot-2', day: 2 },
  { kind: 'toot-0', day: 0 },
];

const url = process.env.EVENTS_URL;
if (!url) throw new Error('missing required environment variable EVENTS_URL');

const res = await fetch(url, { signal: AbortSignal.timeout(15_000) });
if (!res.ok) throw new Error(`events feed responded ${res.status}`);
const events = await res.json();

const sent = await state.load();
const today = new Date().toISOString().slice(0, 10);
let added = 0;

for (const ev of events) {
  const diff = daysUntil(ev.start);
  if (diff === null) continue;
  for (const trigger of OLD_TRIGGERS) {
    if (diff > trigger.day) continue; // trigger day still ahead; let the new code send it
    if (trigger.mail && ev.externalEvent) continue;
    if (trigger.kind === 'announcement' && ev.hideRegistration) continue;
    const entry = state.key(ev.uid, trigger.kind);
    if (sent.has(entry)) continue;
    sent.set(entry, today);
    added++;
    console.log(`seeded ${entry} (event is ${diff}d out)`);
  }
}

await state.save(sent);
console.log(`seeded ${added} entries`);
