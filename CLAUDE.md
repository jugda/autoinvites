# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Sends event invitations for the JUG Darmstadt — email to the mailing list, toots to Mastodon —
driven by the events JSON feed at `EVENTS_URL`. It runs as a scheduled GitHub Actions workflow.

ESM, Node >= 22 (floor set by `masto` 8, which is ESM-only). No build step, no transpiler.

## Commands

```bash
npm install
npm test                       # node:test, no framework
node --test test/config.test.js  # single file
npm start                      # one pass; needs the env in README.md
```

`test/smtp.test.js` runs a real `smtp-server` on a random port and sends a real message
through it, so the mail path is covered without touching a live relay. That is the test to
extend if you change anything about the transport.

Two workflows: `ci.yml` runs the tests on pull requests and pushes to master, `invites.yml`
does the scheduled send. They are separate because a workflow only runs on the triggers it
declares - `invites.yml` has no `pull_request` trigger, so without `ci.yml` a Dependabot PR
would arrive with no checks on it at all.

Deployment is `git push` — there is nothing to build or upload.

## Where this runs

| Piece | Platform |
| --- | --- |
| this job | GitHub Actions (`.github/workflows/invites.yml`) |
| `www.jug-da.de` + `events.json` | GitHub Pages (`jugda/jugda.github.io`) |
| `jug-da.de` apex | still an **AWS S3** redirect bucket → `www` (HTTP only) |
| mail for `jug-da.de` | Uberspace (`MX fenrir.uberspace.de`), not under our control |

Migrated off AWS Lambda + SES in v2. The apex redirect is the last remaining AWS dependency.

## Architecture

`index.js` fetches the feed, computes `daysUntil(ev.start)` for each event, and asks `mail.js`
and `toot.js` independently whether a milestone is due. Sends are awaited and recorded one at a
time; a failure is collected and reported at the end rather than aborting the remaining events.

**Milestones, not exact days.** Each module owns a `MILESTONES` table of `{ kind, from, to }`
day-windows (mail: 28/7/2, toots: 28/7/2/0). A milestone fires anywhere inside its window and is
then recorded in `state/sent.json` by `${uid}:${kind}`, so it never fires twice. This exists
because GitHub's scheduled runs are best-effort — GitHub documents that queued jobs "may be
dropped" — and the old exact-day design (`diff === 28`) silently skipped an invitation whenever a
run was missed. `toot-0` deliberately stays an exact match: it renders "!!!HEUTE!!!".

**State is committed back to the repo** by the workflow. That doubles as the keepalive for
GitHub's rule that scheduled workflows in public repos are disabled after 60 days of inactivity —
if nothing was sent and the repo has been quiet for 25+ days, the workflow pushes an empty commit.
Don't remove that step.

`state.prune()` only drops an entry when the event is gone from the feed **and** the send is over
60 days old. Both conditions — otherwise a truncated or failed feed fetch would wipe state and
cause a storm of re-sends.

### Dates

`dates.js` owns all date handling. Feed timestamps are **naive Berlin wall-clock** with no offset
(`"2026-08-20T18:30:00"`). They are parsed into UTC-anchored `Date`s and formatted with
`timeZone: 'UTC'`, so the printed components are exactly what the feed gave us no matter what TZ
the runner uses. Day counts compare UTC-anchored midnights against "today in Berlin", which keeps
them exact across DST.

`moment` was dropped in v2 for `Intl.DateTimeFormat`. The four German formats were verified
byte-identical over 4 years of dates; `test/dates.test.js` pins the exact expected strings. If you
touch formatting, those strings are the contract — they appear in mail subjects and toots.

### Templates

Handlebars, resolved **relative to the module** (`new URL('./templates/...', import.meta.url)`),
not to the process CWD. Field names differ per channel: mail uses `{{summary}}`, the toot template
uses `{{title}}` / `{{speaker}}` / `{{twitter}}`. Unlike v1, event objects are no longer mutated —
derived fields are passed as a copy into `render()`.

The plain-text mail part comes from `html-to-text`; the old `strip` dependency deleted tags
outright, which dropped every link from the text part.

## Configuration

`config.js` is the only place that reads `process.env`. It validates once at startup and
reports *every* missing variable at once, so a misconfigured run fails before it sends
anything. Everything else takes a plain config object — which is what makes the mail and
Mastodon paths testable without a live endpoint.

Deliberately provider-agnostic: any SMTP host that accepts the configured `MAIL_FROM`
works. See README.md for the full table. Two details worth keeping:

- `Number(env.SMTP_PORT || 587)` uses `||`, not `??`, because an unset GitHub Actions
  variable arrives as an **empty string** rather than undefined. Same reasoning behind the
  empty-string handling in `bool()`. `test/config.test.js` pins this.
- `SMTP_USER`/`SMTP_PASS` are optional as a pair (relays that authorise by IP), but a user
  without a password is treated as a misconfiguration rather than anonymous auth. Mastodon
  is optional the same way — unset both and the toot half just stays off.

SPF/DKIM alignment for whatever relay is configured is handled outside this repo.

## Cutover

`scripts/seed-state.js` is a one-off: it marks every milestone whose old exact trigger day has
already passed as sent, so switching from the Lambda doesn't re-send what it already delivered.
It has been run; `state/sent.json` is seeded. Don't run it again on a live state file without
reading it first.

## Conventions

- `.editorconfig` applies: 2-space indent, UTF-8, LF, final newline, trimmed trailing whitespace.
- All user-facing strings (mail bodies, subjects, toots) are German.
