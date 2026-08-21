import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the mail path against a real SMTP server rather than a mock: the transport
 * connects, authenticates, and the message that actually arrives is inspected.
 */
class SmtpTest {

  private static final Event EVENT = new Event(
      "20260917@jug-da.de",
      "Supercharging Java with Quarkus (Marco Klaassen)",
      "Supercharging Java with Quarkus",
      "abstract",
      "Marco Klaassen",
      "marco",
      "MaibornWolff GmbH, Darmstadt",
      "https://www.jug-da.de/2026/09/Quarkus-LLMs/",
      "2026-09-17T18:30:00",
      false,
      false);

  private GreenMail smtp;

  @BeforeEach
  void startServer() {
    var setup = new ServerSetup(0, "127.0.0.1", ServerSetup.PROTOCOL_SMTP);
    setup.setServerStartupTimeout(5000);
    smtp = new GreenMail(setup);
    smtp.setUser("orga@jug-da.de", "jugda", "s3cret");
    smtp.start();
  }

  @AfterEach
  void stopServer() {
    smtp.stop();
  }

  private Config.Mail config(String password) {
    var env = new HashMap<String, String>();
    env.put("EVENTS_URL", "https://example.invalid/events.json");
    env.put("MAIL_TO", "liste@jug-da.de");
    env.put("MAIL_FROM", "orga@jug-da.de");
    env.put("SMTP_HOST", "127.0.0.1");
    env.put("SMTP_PORT", String.valueOf(smtp.getSmtp().getPort()));
    env.put("SMTP_USER", "jugda");
    env.put("SMTP_PASS", password);
    return Config.load(env).mail();
  }

  @Test
  @DisplayName("delivers a real message through a plain authenticated SMTP server")
  void deliversRealMessage() throws Exception {
    var milestone = Mail.due(EVENT, 7).orElseThrow();
    var messageId = Mail.send(EVENT, milestone, config("s3cret"));
    assertNotNull(messageId);

    assertTrue(smtp.waitForIncomingEmail(5000, 1), "no message arrived");
    var received = smtp.getReceivedMessages();
    assertEquals(1, received.length);

    var message = received[0];
    assertEquals("orga@jug-da.de", message.getFrom()[0].toString());
    assertEquals("liste@jug-da.de", message.getAllRecipients()[0].toString());
    assertEquals("17.09.2026: Supercharging Java with Quarkus (Marco Klaassen)", message.getSubject());

    assertTrue(message.getContentType().toLowerCase().contains("multipart/alternative"), message.getContentType());
    var body = (MimeMultipart) message.getContent();
    assertEquals(2, body.getCount(), "expected a text and an html part");
    assertTrue(body.getBodyPart(0).getContentType().toLowerCase().contains("text/plain"));
    assertTrue(body.getBodyPart(1).getContentType().toLowerCase().contains("text/html"));

    var text = body.getBodyPart(0).getContent().toString();
    assertTrue(text.contains("Donnerstag, 17. September 2026"), text);
    assertTrue(text.contains("18:30"), text);
    // The link target has to survive into the plain-text part.
    assertTrue(text.contains("https://www.jug-da.de/2026/09/Quarkus-LLMs/"), text);
  }

  @Test
  @DisplayName("the announcement is a different template with its own subject prefix")
  void announcementDiffers() throws Exception {
    var milestone = Mail.due(EVENT, 28).orElseThrow();
    Mail.send(EVENT, milestone, config("s3cret"));

    assertTrue(smtp.waitForIncomingEmail(5000, 1), "no message arrived");
    var message = smtp.getReceivedMessages()[0];
    assertTrue(message.getSubject().startsWith("Ankündigung für 17.09.2026:"), message.getSubject());

    var body = (MimeMultipart) message.getContent();
    assertTrue(body.getBodyPart(0).getContent().toString().contains("Die Anmeldung ist jetzt ebenfalls geöffnet"));
  }

  @Test
  @DisplayName("rejects a wrong password rather than silently dropping the mail")
  void wrongPasswordFailsLoudly() {
    var milestone = Mail.due(EVENT, 7).orElseThrow();
    assertThrows(Exception.class, () -> Mail.send(EVENT, milestone, config("wrong")));
    assertEquals(0, smtp.getReceivedMessages().length);
  }
}
