# JUG DA Autoinvites

Sends event invitations for the [JUG Darmstadt](https://www.jug-da.de) — by email to the
mailing list and as toots to Mastodon — based on the events JSON feed published by
[jugda.github.io](https://github.com/jugda/jugda.github.io).

A [JBang](https://www.jbang.dev) script - no build tool, no `pom.xml`, dependencies declared
inline with `//DEPS`. Runs as a scheduled GitHub Actions workflow
(`.github/workflows/invites.yml`) Monday to Saturday, mid-morning, Europe/Berlin.

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

"Set in" is where the workflow reads each one from, under
*Settings → Secrets and variables → Actions*:

| Variable | Set in | Required | Notes |
| --- | --- | --- | --- |
| `EVENTS_URL` | variable | yes | e.g. `https://www.jug-da.de/events.json` |
| `SMTP_HOST` | variable | yes | hostname of the relay |
| `SMTP_PORT` | variable | no | default `587` |
| `SMTP_SECURE` | variable | no | implicit TLS; defaults to true on 465, false otherwise |
| `MASTO_URL` | variable | no | omit with `MASTO_TOKEN` to disable tooting |
| `MAIL_TO` | secret | yes | recipient (the mailing list) |
| `MAIL_FROM` | secret | yes | sender; the SMTP host must allow it |
| `SMTP_USER` | secret | no | omit with `SMTP_PASS` for a relay that authorises by IP |
| `SMTP_PASS` | secret | with `SMTP_USER` | |
| `MASTO_TOKEN` | secret | no | access token with `write:statuses` |
| `DRY_RUN` | — | no | workflow_dispatch input, not stored; log intended sends and send nothing |
| `STATE_FILE` | — | no | defaults to `state/sent.json`; mainly for local runs |

Config is validated once at startup and reports *all* problems at once, so a bad setup
fails before anything is sent rather than halfway through.

The variable/secret split only affects log masking — which matters here, because Actions
logs on a public repo are world-readable. The addresses are secrets for that reason rather
than because they are confidential. The code reads plain environment variables and cannot
tell the difference, so move any of them between the two as you see fit.

## Running it locally

Needs `jbang` and a JDK 25 (`sdk install java 25.0.2-tem`, `sdk install jbang`). JBang
resolves the dependencies on first run.

```bash
jbang test/Tests.java          # the whole suite
./scripts/run-tests.sh         # same thing

# see what would go out, without sending or recording anything
DRY_RUN=true EVENTS_URL=https://www.jug-da.de/events.json \
  MAIL_TO=you@example.com MAIL_FROM=you@example.com SMTP_HOST=localhost \
  jbang src/Autoinvites.java
```

Run from the repository root: `state/sent.json` is resolved against the working directory
(override with `STATE_FILE`). The templates are not - they travel inside the built jar via
the `//FILES` directive.

`workflow_dispatch` on the workflow does the same from the Actions tab, with the dry-run
box ticked by default.

## Cutover from the old AWS Lambda

`scripts/seed-state.js` marks every milestone whose old exact trigger day has already
passed as sent, so switching over doesn't re-send what the Lambda already delivered. Run it
once, immediately before the first live run:

```bash
EVENTS_URL=https://www.jug-da.de/events.json \
  MAIL_TO=x MAIL_FROM=y SMTP_HOST=z jbang scripts/SeedState.java
```
