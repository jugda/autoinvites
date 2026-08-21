// All feed timestamps are naive Berlin wall-clock ("2026-08-20T18:30:00", no offset).
// We anchor them at UTC and format in UTC, so the components we print are exactly the
// ones the feed gave us — independent of the TZ the runner happens to use.

const BERLIN = 'Europe/Berlin';
const NAIVE = /^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2})(?::(\d{2}))?)?$/;

const fmt = (opts) => new Intl.DateTimeFormat('de-DE', { timeZone: 'UTC', ...opts });

const SHORT_DATE = fmt({ day: '2-digit', month: '2-digit', year: 'numeric' });
const LONG_DATE = fmt({ weekday: 'long', day: '2-digit', month: 'long', year: 'numeric' });
const TIME = fmt({ hour: '2-digit', minute: '2-digit', hour12: false });
const DAY_MONTH = fmt({ day: '2-digit', month: '2-digit' });

export const parseStart = (value) => {
  const m = NAIVE.exec(String(value ?? '').trim());
  if (!m) return null;
  const [, y, mo, d, h = '0', mi = '0', s = '0'] = m;
  return new Date(Date.UTC(+y, +mo - 1, +d, +h, +mi, +s));
};

/** Midnight of "today in Berlin", anchored at UTC for exact day arithmetic. */
export const berlinToday = (now = new Date()) => {
  const parts = Object.fromEntries(
    new Intl.DateTimeFormat('en-CA', {
      timeZone: BERLIN, year: 'numeric', month: '2-digit', day: '2-digit',
    }).formatToParts(now).map(({ type, value }) => [type, value]),
  );
  return new Date(Date.UTC(+parts.year, +parts.month - 1, +parts.day));
};

/** Whole days from today (Berlin) until the event's calendar day. Null if unparseable. */
export const daysUntil = (start, now = new Date()) => {
  const at = parseStart(start);
  if (!at) return null;
  const day = Date.UTC(at.getUTCFullYear(), at.getUTCMonth(), at.getUTCDate());
  return Math.round((day - berlinToday(now).getTime()) / 86_400_000);
};

export const shortDate = (at) => SHORT_DATE.format(at);   // 20.08.2026
export const longDate = (at) => LONG_DATE.format(at);     // Donnerstag, 20. August 2026
export const time = (at) => TIME.format(at);              // 18:30
export const dayMonth = (at) => DAY_MONTH.format(at);     // 20.08.
