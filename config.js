// Every external endpoint is configured from the environment; nothing is hard-coded to a
// particular provider. Validation happens once, up front, so a misconfigured run fails
// before it sends anything — and reports *all* missing variables at once rather than
// dying on the first one.

const REQUIRED = ['EVENTS_URL', 'MAIL_TO', 'MAIL_FROM', 'SMTP_HOST'];

const bool = (value, fallback) =>
  value === undefined || value === '' ? fallback : /^(1|true|yes|on)$/i.test(value);

export const load = (env = process.env) => {
  const missing = REQUIRED.filter((name) => !env[name]);

  // Mastodon is optional: leave the token unset and the toot half simply stays off.
  const mastodon = env.MASTO_URL && env.MASTO_TOKEN
    ? { url: env.MASTO_URL, accessToken: env.MASTO_TOKEN }
    : null;
  if ((env.MASTO_URL || env.MASTO_TOKEN) && !mastodon) {
    missing.push(env.MASTO_URL ? 'MASTO_TOKEN' : 'MASTO_URL');
  }

  // SMTP auth is optional too, for relays that authorise by IP instead of credentials.
  if (env.SMTP_USER && !env.SMTP_PASS) missing.push('SMTP_PASS');

  if (missing.length) {
    throw new Error(`missing required environment variable(s): ${missing.join(', ')}`);
  }

  // `||`, not `??`: an unset GitHub Actions variable arrives as an empty string.
  const port = Number(env.SMTP_PORT || 587);
  if (!Number.isInteger(port) || port < 1 || port > 65535) {
    throw new Error(`SMTP_PORT must be a port number, got ${JSON.stringify(env.SMTP_PORT)}`);
  }

  return {
    eventsUrl: env.EVENTS_URL,
    dryRun: bool(env.DRY_RUN, false),
    mail: {
      to: env.MAIL_TO,
      from: env.MAIL_FROM,
      smtp: {
        host: env.SMTP_HOST,
        port,
        // 465 is implicit TLS, 587/25 start plain and upgrade via STARTTLS.
        // Override with SMTP_SECURE for hosts that don't follow the convention.
        secure: bool(env.SMTP_SECURE, port === 465),
        auth: env.SMTP_USER ? { user: env.SMTP_USER, pass: env.SMTP_PASS } : undefined,
      },
    },
    mastodon,
  };
};
