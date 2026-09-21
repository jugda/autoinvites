# JUG DA Autoinvites

Sends event invitations for the [JUG Darmstadt](https://www.jug-da.de) — by email to the
mailing list and as toots to Mastodon — based on the events JSON feed published by
[jugda.github.io](https://github.com/jugda/jugda.github.io).

Runs as a scheduled workflow Monday to Saturday, mid-morning, Europe/Berlin — on Forgejo
Actions (`.forgejo/workflows/invites.yml`) and on GitHub Actions
(`.github/workflows/invites.yml`). See [Running on Forgejo](#running-on-forgejo).

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

"Set in" is where the workflow reads each one from — *Settings → Actions → Secrets* and
*→ Variables* on Forgejo, *Settings → Secrets and variables → Actions* on GitHub:

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

Config is validated once at startup and reports *all* problems at once, so a bad setup
fails before anything is sent rather than halfway through.

The variable/secret split only affects log masking — which matters here, because Actions
logs on a public repo are world-readable. The addresses are secrets for that reason rather
than because they are confidential. The code reads plain environment variables and cannot
tell the difference, so move any of them between the two as you see fit.

## Running on Forgejo

The workflows exist twice: `.forgejo/workflows/` for Forgejo Actions, `.github/workflows/`
for GitHub Actions. Forgejo ignores `.github/workflows` entirely as soon as
`.forgejo/workflows` exists, and GitHub never looks at `.forgejo`, so the two sets coexist
without either forge picking up the other's copy. The flip side is that a change to one has
to be made in the other by hand.

**Only one forge may have a live schedule.** Both copies send the same invitations from the
same feed, each recording into its own `state/sent.json` history — with secrets configured
on both, every mail and toot goes out twice. Set the variables and secrets on one forge only.

Setting it up on a Forgejo instance:

1. Actions have to be enabled for the instance (`[actions] ENABLED = true`) and for this
   repository (*Settings → Repository → Advanced Settings → Enable Repository Actions*).
2. A runner has to be registered with the label `ubuntu-latest`, which is what our
   instance uses. It is a runner label, not a GitHub image name — forgejo-runner's own
   default label is `docker`. If your runners are labelled differently, change `runs-on:`
   in both files under `.forgejo/workflows/`.
3. Add the variables and secrets from the table above under *Settings → Actions*.
4. Schedules are only registered from the default branch, so these files have to be on
   `main` before anything fires. *Actions → Send invites → Run workflow* does a manual run,
   with the dry-run box ticked by default.

The "Persist state" step commits `state/sent.json` back using the automatic token, which
therefore needs write access to the repository. If the instance hands out read-only tokens,
add a personal access token with `write:repository` as a secret and pass it to
`actions/checkout` as `token:`.

Actions are referenced by full URL (`https://data.forgejo.org/actions/checkout@v7`) instead
of the bare `actions/checkout@v7`, because the short form resolves against whatever
`DEFAULT_ACTIONS_URL` the instance happens to be configured with.

`.github/dependabot.yml` is GitHub-only — Forgejo has no equivalent, so dependency bumps on
that side are manual unless the instance runs a Renovate bot.

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
