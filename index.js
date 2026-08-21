import * as mail from './mail.js';
import * as state from './state.js';
import * as toot from './toot.js';
import { daysUntil } from './dates.js';

const DRY_RUN = /^(1|true|yes)$/i.test(process.env.DRY_RUN ?? '');

const fetchEvents = async (url) => {
  const res = await fetch(url, { signal: AbortSignal.timeout(15_000) });
  if (!res.ok) throw new Error(`events feed ${url} responded ${res.status} ${res.statusText}`);
  const data = await res.json();
  if (!Array.isArray(data)) throw new Error('events feed did not return an array');
  return data;
};

const main = async () => {
  const url = process.env.EVENTS_URL;
  if (!url) throw new Error('missing required environment variable EVENTS_URL');

  const events = await fetchEvents(url);
  const sent = await state.load();
  const failures = [];
  let delivered = 0;

  console.log(`${events.length} events in feed${DRY_RUN ? ' (DRY_RUN: nothing will be sent)' : ''}`);

  for (const ev of events) {
    const diff = daysUntil(ev.start);
    if (diff === null) continue;

    const jobs = [];
    const mailMilestone = mail.due(ev, diff);
    if (mailMilestone) {
      jobs.push({ kind: mailMilestone.kind, run: () => mail.send(ev, mailMilestone) });
    }
    const tootMilestone = toot.due(ev, diff);
    if (tootMilestone) {
      jobs.push({ kind: tootMilestone.kind, run: () => toot.send(ev, diff) });
    }

    for (const job of jobs) {
      const entry = state.key(ev.uid, job.kind);
      if (sent.has(entry)) continue;

      if (DRY_RUN) {
        console.log(`would send ${job.kind} for ${ev.uid} (in ${diff}d)`);
        continue;
      }
      try {
        const ref = await job.run();
        // Recorded only after the send actually resolved, so a failure retries tomorrow.
        sent.set(entry, new Date().toISOString().slice(0, 10));
        delivered++;
        console.log(`sent ${job.kind} for ${ev.uid} (in ${diff}d) -> ${ref}`);
      } catch (err) {
        failures.push(`${job.kind} for ${ev.uid}: ${err.message}`);
        console.error(`FAILED ${job.kind} for ${ev.uid}:`, err);
      }
    }
  }

  if (!DRY_RUN) {
    state.prune(sent, new Set(events.map((ev) => ev.uid)));
    await state.save(sent);
  }

  console.log(`done: ${delivered} sent, ${failures.length} failed`);
  if (failures.length) throw new AggregateError([], `${failures.length} send(s) failed:\n  ${failures.join('\n  ')}`);
};

await main();
