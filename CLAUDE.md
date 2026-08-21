# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Sends event invitations for the JUG Darmstadt — email to the mailing list, toots to Mastodon —
driven by the events JSON feed at `EVENTS_URL`. It runs as a scheduled GitHub Actions workflow.

A **JBang** script on **Java 25**: no build tool, no `pom.xml`, no wrapper. Dependencies are
`//DEPS` lines in `src/Autoinvites.java`. Ported from Node in v3; the git history of the
JavaScript version is on the `migrate-to-github-actions` branch.

## Commands

```bash
jbang test/Tests.java                 # whole suite (41 tests)
jbang src/Autoinvites.java            # one pass; needs the env in README.md
jbang scripts/SeedState.java          # one-off cutover helper

# a single test class
jbang test/Tests.java execute --select-class=DatesTest --details=tree
```

There is no build step. `jbang build src/Autoinvites.java` only warms the cache.

Two workflows: `ci.yml` runs the tests on pull requests and pushes to `main`, `invites.yml`
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

Migrated off AWS Lambda + SES in v2, off Node in v3. The apex redirect is the last remaining
AWS dependency.

## Layout

```
src/         the classes; src/Autoinvites.java is the entry point and holds //DEPS + //FILES
templates/   Handlebars mail + toot bodies, edited by the orga
state/       sent.json, committed back by the workflow - data, not source
scripts/     one-off operational scripts
test/        JUnit 5; test/Tests.java is the runner
```

Everything is in the **default package** — JBang convention, and `//SOURCES` globs stay simple.

## JBang specifics worth knowing

These were each established by experiment; don't rediscover them the hard way.

- **`//SOURCES ../src/*.java` aggregates directives too.** `test/Tests.java` inherits the
  production `//DEPS` and `//FILES` from `src/Autoinvites.java`, so there is one dependency
  list, not two.
- **`Tests.java` has its own `main`.** JBang picks the main class from the compiled set, and
  `Autoinvites` also has one; passing `--main` is *ignored* once the jar is cached with a
  different `Main-Class`. Giving the runner its own `main` is what makes `jbang test/Tests.java`
  reliable. Do not remove it.
- **Test discovery.** JBang has no `test` command, and a bare `--scan-classpath` finds nothing
  because the classes live in a jar in JBang's cache. `Tests.main` locates that jar from its own
  code source and scans it, so adding a test class needs no registration.
- **Templates travel in the jar.** `//FILES templates/=../templates/` plus
  `ClassPathTemplateLoader` means rendering does not depend on the working directory —
  verified by running the script from `/`.
- **`state/sent.json` *is* working-directory relative**, deliberately: it is a file in the
  checkout that the workflow commits back, not a bundled resource. `STATE_FILE` overrides it.

## Architecture

`Autoinvites.main` fetches the feed, computes `Dates.daysUntil` per event, and asks `Mail` and
`Toot` independently whether a milestone is due. Sends are awaited one at a time; a failure is
collected and rethrown at the end rather than aborting the remaining events.

**Milestones, not exact days.** `Mail` and `Toot` each own a `List<Milestone>` of
`{kind, template, from, to}` day-windows (mail 28/7/2, toots 28/7/2/0). A milestone fires
anywhere inside its window and is then recorded in `state/sent.json` under `uid:kind`, so it
never fires twice. This exists because GitHub's scheduled runs are best-effort — GitHub
documents that queued jobs "may be dropped" — and the original exact-day design silently
skipped an invitation whenever a run was missed. `toot-0` stays an exact match: it renders
"!!!HEUTE!!!". The windows deliberately do not touch, which `MilestonesTest` asserts at the gaps.

**State is committed back** by the workflow. That doubles as the keepalive for GitHub's rule
that scheduled workflows in public repos are disabled after 60 days of inactivity — if nothing
was sent and the repo has been quiet 25+ days, it pushes an empty commit. Don't remove that step.

`State.prune` only drops an entry when the event is gone from the feed **and** the send is over
60 days old. Both conditions — otherwise a truncated or failed feed fetch would wipe state and
cause a storm of re-sends.

### Dates

Feed timestamps are naive Berlin wall-clock with no offset (`"2026-08-20T18:30:00"`), which is
exactly `LocalDateTime`. This is the one place Java is markedly better than the JavaScript it
replaced, which needed UTC-anchoring tricks to stop the host timezone leaking into output.

The four German formats are pinned in `DatesTest` as the exact strings the previous
implementations produced. They appear in mail subjects and toots — treat them as a contract.

### Mail

Jakarta Mail via `angus-mail`, any SMTP host. `Html.toPlainText` builds the text/plain part with
jsoup and **must keep link targets**: the original `strip` dependency merely removed tags, which
silently dropped every URL from the text part.

`SmtpTest` runs a real GreenMail SMTP server and asserts on the message that actually arrives —
envelope, subject, both MIME parts, and a wrong password failing loudly. That is the test to
extend if you touch the transport.

## Configuration

`Config.load(Map)` is the only reader of the environment. It validates once and reports *every*
missing variable at once. Everything else takes a plain `Config` record, which is what makes the
mail and Mastodon paths testable without a live endpoint. See README.md for the full table.

Blank counts as absent throughout: an unset GitHub Actions variable arrives as an **empty
string**, not as missing. `ConfigTest` pins this. `SMTP_USER`/`SMTP_PASS` and
`MASTO_URL`/`MASTO_TOKEN` are optional in full-or-nothing pairs; half a pair is an error.

SPF/DKIM alignment for whatever relay is configured is handled outside this repo.

## Dependency updates

Dependabot covers the actions only. It has **no ecosystem for JBang `//DEPS`**, and the Maven
ecosystem needs a `pom.xml` this project does not have. Renovate does understand JBang scripts,
or check by hand with `jbang deps@jbangdev src/Autoinvites.java`.

## Cutover

`scripts/SeedState.java` marks every milestone whose *old* exact trigger day has already passed
as sent, so switching over doesn't re-send what the previous implementation delivered. Its
`OLD_TRIGGERS` table is a deliberate historical record — don't refactor it to share the current
milestone windows.

## Conventions

- `.editorconfig` applies: 2-space indent, UTF-8, LF, final newline, trimmed trailing whitespace.
- All user-facing strings (mail bodies, subjects, toots) are German.
