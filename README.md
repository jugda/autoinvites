# JUG DA Autoinvites

Sends event invitations for the [JUG Darmstadt](https://www.jug-da.de) — by email to the
mailing list and as toots to Mastodon — based on the events JSON feed published by
[jugda.github.io](https://github.com/jugda/jugda.github.io).

Runs as a scheduled GitHub Actions workflow (`.github/workflows/invites.yml`) on weekday
mornings, Europe/Berlin.

## What gets sent when

| Days before event | Mail | Toot |
| --- | --- | --- |
| 28 | announcement (incl. "registration is open") | yes |
| 7 | invitation | yes |
| 2 | invitation | yes |
| 0 | — | yes ("!!!HEUTE!!!") |

Events flagged `externalEvent` get toots but no mail. `hideRegistration` suppresses the
28-day announcement mail only.

Each milestone actually fires anywhere in a small window (e.g. 26–28 days) and is recorded
in `state/sent.json`, so a missed or dropped scheduled run catches up the next day and
nothing is ever sent twice. The same-day toot is the exception — it stays an exact match,
since it would be wrong a day late.

## Configuration

Everything is read from the environment. Nothing is tied to a particular mail provider —
point it at any SMTP server that will accept your `MAIL_FROM`.

| Variable | Required | Notes |
| --- | --- | --- |
| `EVENTS_URL` | yes | e.g. `https://www.jug-da.de/events.json` |
| `MAIL_TO` | yes | recipient (the mailing list) |
| `MAIL_FROM` | yes | sender; the SMTP host must allow it |
| `SMTP_HOST` | yes | hostname of the relay |
| `SMTP_PORT` | no | default `587` |
| `SMTP_SECURE` | no | implicit TLS; defaults to true on 465, false otherwise |
| `SMTP_USER` | no | omit with `SMTP_PASS` for a relay that authorises by IP |
| `SMTP_PASS` | with `SMTP_USER` | |
| `MASTO_URL` | no | omit with `MASTO_TOKEN` to disable tooting |
| `MASTO_TOKEN` | no | access token with `write:statuses` |
| `DRY_RUN` | no | log intended sends; send and record nothing |

Config is validated once at startup and reports *all* problems at once, so a bad setup
fails before anything is sent rather than halfway through.

In the workflow the non-sensitive ones are repository **variables** and the rest are
**secrets**. That split only affects log masking — which matters, because Actions logs on
a public repo are world-readable. The code reads plain environment variables, so move any
of them between the two as you see fit.

## Running it locally

```bash
npm install
npm test

# see what would go out, without sending or recording anything
DRY_RUN=true EVENTS_URL=https://www.jug-da.de/events.json \
  MAIL_TO=you@example.com MAIL_FROM=you@example.com SMTP_HOST=localhost \
  npm start
```

`workflow_dispatch` on the workflow does the same from the Actions tab, with the dry-run
box ticked by default.

## Cutover from the old AWS Lambda

`scripts/seed-state.js` marks every milestone whose old exact trigger day has already
passed as sent, so switching over doesn't re-send what the Lambda already delivered. Run it
once, immediately before the first live run:

```bash
EVENTS_URL=https://www.jug-da.de/events.json node scripts/seed-state.js
```
