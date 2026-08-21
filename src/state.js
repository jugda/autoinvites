// Which invitations have already gone out.
//
// The old design matched day-offsets exactly (diff === 2 | 7 | 28), so a single missed
// run silently skipped an invitation and a re-run double-sent. GitHub Actions schedules
// are best-effort ("some queued jobs may be dropped"), so we record what we sent instead
// and let each milestone catch up over a small window. See MILESTONES in mail.js/toot.js.

import { readFile, writeFile, mkdir } from 'node:fs/promises';

const FILE = new URL('../state/sent.json', import.meta.url);
const KEEP_UNKNOWN_FOR_DAYS = 60;

export const load = async () => {
  try {
    const parsed = JSON.parse(await readFile(FILE, 'utf-8'));
    return new Map(Object.entries(parsed.sent ?? {}));
  } catch (err) {
    if (err.code === 'ENOENT') return new Map();
    throw err;
  }
};

export const key = (uid, kind) => `${uid}:${kind}`;

/**
 * Drop entries whose event has fallen out of the feed *and* whose send is old enough
 * that it cannot still be relevant. Both conditions, so a truncated or failed feed
 * fetch can never wipe state and cause a storm of re-sends.
 */
export const prune = (sent, liveUids, now = new Date()) => {
  const cutoff = new Date(now.getTime() - KEEP_UNKNOWN_FOR_DAYS * 86_400_000)
    .toISOString().slice(0, 10);
  for (const [entry, sentOn] of sent) {
    const uid = entry.slice(0, entry.lastIndexOf(':'));
    if (!liveUids.has(uid) && sentOn < cutoff) sent.delete(entry);
  }
  return sent;
};

export const save = async (sent) => {
  await mkdir(new URL('../state/', import.meta.url), { recursive: true });
  const ordered = Object.fromEntries([...sent].sort(([a], [b]) => a.localeCompare(b)));
  await writeFile(FILE, `${JSON.stringify({ version: 1, sent: ordered }, null, 2)}\n`, 'utf-8');
};
