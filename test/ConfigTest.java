import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigTest {

  private static Map<String, String> base() {
    var env = new HashMap<String, String>();
    env.put("EVENTS_URL", "https://example.invalid/events.json");
    env.put("MAIL_TO", "liste@jug-da.de");
    env.put("MAIL_FROM", "orga@jug-da.de");
    env.put("SMTP_HOST", "smtp.example.invalid");
    env.put("SMTP_USER", "user");
    env.put("SMTP_PASS", "pass");
    return env;
  }

  private static Map<String, String> with(String key, String value) {
    var env = base();
    env.put(key, value);
    return env;
  }

  @Test
  @DisplayName("reports every missing variable at once, not just the first")
  void reportsAllMissing() {
    var thrown = assertThrows(IllegalStateException.class,
        () -> Config.load(Map.of("SMTP_HOST", "smtp.example.invalid")));
    assertTrue(thrown.getMessage().contains("EVENTS_URL"), thrown.getMessage());
    assertTrue(thrown.getMessage().contains("MAIL_TO"), thrown.getMessage());
    assertTrue(thrown.getMessage().contains("MAIL_FROM"), thrown.getMessage());
  }

  @Test
  @DisplayName("defaults to submission on 587 with STARTTLS")
  void defaultsToSubmission() {
    var smtp = Config.load(base()).mail().smtp();
    assertEquals(587, smtp.port());
    assertFalse(smtp.secure());
    assertTrue(smtp.authenticates());
  }

  @Test
  @DisplayName("treats 465 as implicit TLS")
  void implicitTlsOn465() {
    assertTrue(Config.load(with("SMTP_PORT", "465")).mail().smtp().secure());
  }

  @Test
  @DisplayName("SMTP_SECURE overrides the port convention in both directions")
  void secureOverride() {
    var forced = base();
    forced.put("SMTP_PORT", "587");
    forced.put("SMTP_SECURE", "true");
    assertTrue(Config.load(forced).mail().smtp().secure());

    var disabled = base();
    disabled.put("SMTP_PORT", "465");
    disabled.put("SMTP_SECURE", "false");
    assertFalse(Config.load(disabled).mail().smtp().secure());
  }

  @Test
  @DisplayName("rejects a nonsense port rather than failing later at connect time")
  void rejectsBadPort() {
    assertThrows(IllegalStateException.class, () -> Config.load(with("SMTP_PORT", "submission")));
    assertThrows(IllegalStateException.class, () -> Config.load(with("SMTP_PORT", "99999")));
  }

  @Test
  @DisplayName("allows a relay that authorises by IP instead of credentials")
  void allowsUnauthenticatedRelay() {
    var env = base();
    env.remove("SMTP_USER");
    env.remove("SMTP_PASS");
    assertFalse(Config.load(env).mail().smtp().authenticates());
  }

  @Test
  @DisplayName("a username without a password is a misconfiguration, not anonymous auth")
  void halfCredentialsRejected() {
    var env = base();
    env.remove("SMTP_PASS");
    assertThrows(IllegalStateException.class, () -> Config.load(env));
  }

  @Test
  @DisplayName("Mastodon is optional but must be configured in full")
  void mastodonAllOrNothing() {
    assertNull(Config.load(base()).mastodon());
    assertThrows(IllegalStateException.class, () -> Config.load(with("MASTO_URL", "https://m.example")));
    assertThrows(IllegalStateException.class, () -> Config.load(with("MASTO_TOKEN", "t")));

    var full = base();
    full.put("MASTO_URL", "https://m.example");
    full.put("MASTO_TOKEN", "t");
    assertEquals("https://m.example", Config.load(full).mastodon().url());
  }

  @Test
  @DisplayName("DRY_RUN accepts the usual spellings and defaults off")
  void dryRunSpellings() {
    assertFalse(Config.load(base()).dryRun());
    for (var yes : new String[] {"true", "TRUE", "1", "yes", "on"}) {
      assertTrue(Config.load(with("DRY_RUN", yes)).dryRun(), yes);
    }
    for (var no : new String[] {"false", "0", "no", ""}) {
      assertFalse(Config.load(with("DRY_RUN", no)).dryRun(), no);
    }
  }

  @Test
  @DisplayName("an unset Actions variable arrives as an empty string, not as absent")
  void blankCountsAsAbsent() {
    var env = base();
    env.put("SMTP_PORT", "");
    env.put("SMTP_SECURE", "");
    env.put("MASTO_URL", "");
    env.put("MASTO_TOKEN", "");
    var config = Config.load(env);
    assertEquals(587, config.mail().smtp().port());
    assertFalse(config.mail().smtp().secure());
    assertNull(config.mastodon());
  }

  @Test
  @DisplayName("the state file defaults into the checkout and can be redirected")
  void stateFileConfigurable() {
    assertEquals("state/sent.json", Config.load(base()).stateFile().toString());
    assertEquals("/tmp/x.json", Config.load(with("STATE_FILE", "/tmp/x.json")).stateFile().toString());
  }
}
