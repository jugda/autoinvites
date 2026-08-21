import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Every external endpoint comes from the environment; nothing is tied to a particular mail
 * provider. Validation happens once, up front, and reports <em>all</em> missing variables at
 * once rather than dying on the first one.
 */
public record Config(String eventsUrl, boolean dryRun, java.nio.file.Path stateFile, Mail mail, Mastodon mastodon) {

  public record Smtp(String host, int port, boolean secure, String user, String password) {
    public boolean authenticates() {
      return user != null && !user.isBlank();
    }
  }

  public record Mail(String to, String from, Smtp smtp) {
  }

  public record Mastodon(String url, String accessToken) {
  }

  private static final List<String> REQUIRED = List.of("EVENTS_URL", "MAIL_TO", "MAIL_FROM", "SMTP_HOST");

  /** Blank counts as absent: an unset GitHub Actions variable arrives as an empty string. */
  private static String value(Map<String, String> env, String name) {
    var raw = env.get(name);
    return raw == null || raw.isBlank() ? null : raw.trim();
  }

  private static boolean flag(Map<String, String> env, String name, boolean fallback) {
    var raw = value(env, name);
    return raw == null ? fallback : raw.matches("(?i)1|true|yes|on");
  }

  public static Config load(Map<String, String> env) {
    var missing = new ArrayList<String>();
    for (var name : REQUIRED) {
      if (value(env, name) == null) {
        missing.add(name);
      }
    }

    // Mastodon is optional, but half-configured is a mistake rather than "tooting off".
    var mastoUrl = value(env, "MASTO_URL");
    var mastoToken = value(env, "MASTO_TOKEN");
    if (mastoUrl != null && mastoToken == null) {
      missing.add("MASTO_TOKEN");
    }
    if (mastoToken != null && mastoUrl == null) {
      missing.add("MASTO_URL");
    }

    // SMTP auth is optional too, for relays that authorise by IP instead of credentials.
    var smtpUser = value(env, "SMTP_USER");
    var smtpPassword = value(env, "SMTP_PASS");
    if (smtpUser != null && smtpPassword == null) {
      missing.add("SMTP_PASS");
    }

    if (!missing.isEmpty()) {
      throw new IllegalStateException("missing required environment variable(s): " + String.join(", ", missing));
    }

    var rawPort = value(env, "SMTP_PORT");
    int port;
    try {
      port = rawPort == null ? 587 : Integer.parseInt(rawPort);
    } catch (NumberFormatException e) {
      throw new IllegalStateException("SMTP_PORT must be a port number, got \"" + rawPort + "\"", e);
    }
    if (port < 1 || port > 65535) {
      throw new IllegalStateException("SMTP_PORT must be a port number, got \"" + rawPort + "\"");
    }

    // 465 is implicit TLS, 587 and 25 start plain and upgrade via STARTTLS. SMTP_SECURE
    // overrides that convention for hosts which do not follow it.
    var smtp = new Smtp(value(env, "SMTP_HOST"), port, flag(env, "SMTP_SECURE", port == 465), smtpUser, smtpPassword);

    // Relative to the working directory on purpose: unlike the templates this is not a
    // bundled resource but a file in the checkout that the workflow commits back.
    var stateFile = java.nio.file.Path.of(
        value(env, "STATE_FILE") == null ? "state/sent.json" : value(env, "STATE_FILE"));

    return new Config(
        value(env, "EVENTS_URL"),
        flag(env, "DRY_RUN", false),
        stateFile,
        new Mail(value(env, "MAIL_TO"), value(env, "MAIL_FROM"), smtp),
        mastoUrl == null ? null : new Mastodon(mastoUrl, mastoToken));
  }
}
