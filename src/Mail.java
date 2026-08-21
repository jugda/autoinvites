import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public final class Mail {

  /**
   * Windows rather than exact days, so a dropped scheduled run catches up tomorrow.
   * These deliberately do not touch, which {@code MilestonesTest} asserts at the gaps.
   */
  private static final List<Milestone> MILESTONES = List.of(
      new Milestone("announcement", "mail_announcement", 26, 28),
      new Milestone("invitation-7", "mail_invitation", 5, 7),
      new Milestone("invitation-2", "mail_invitation", 1, 2));

  private Mail() {
  }

  /** The milestone due for this event today, if any. */
  public static Optional<Milestone> due(Event event, int daysUntil) {
    if (event.externalEvent()) {
      return Optional.empty();
    }
    return MILESTONES.stream()
        .filter(milestone -> milestone.covers(daysUntil))
        // The announcement doubles as the "registration is open" mail.
        .filter(milestone -> !(milestone.kind().equals("announcement") && event.hideRegistration()))
        .findFirst();
  }

  public static String render(Event event, Milestone milestone, LocalDateTime at) {
    return Templates.render(milestone.template(),
        event.model(Map.of("date", Dates.longDate(at), "time", Dates.time(at))));
  }

  public static String subject(Event event, Milestone milestone, LocalDateTime at) {
    var prefix = milestone.kind().equals("announcement") ? "Ankündigung für " : "";
    return prefix + Dates.shortDate(at) + ": " + event.summary();
  }

  public static String send(Event event, Milestone milestone, Config.Mail config) throws Exception {
    var at = Dates.parseStart(event.start()).orElseThrow();
    var html = render(event, milestone, at);

    var message = new MimeMessage(session(config.smtp()));
    message.setFrom(new InternetAddress(config.from()));
    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(config.to()));
    message.setSubject(subject(event, milestone, at), "UTF-8");

    var text = new MimeBodyPart();
    text.setText(Html.toPlainText(html), "UTF-8");
    var htmlPart = new MimeBodyPart();
    htmlPart.setContent(html, "text/html; charset=UTF-8");

    var body = new MimeMultipart("alternative");
    body.addBodyPart(text);
    body.addBodyPart(htmlPart);
    message.setContent(body);

    Transport.send(message);
    return message.getMessageID();
  }

  private static Session session(Config.Smtp smtp) {
    var properties = new Properties();
    properties.put("mail.transport.protocol", "smtp");
    properties.put("mail.smtp.host", smtp.host());
    properties.put("mail.smtp.port", String.valueOf(smtp.port()));
    if (smtp.secure()) {
      properties.put("mail.smtp.ssl.enable", "true");
    } else {
      // Upgrade when the server offers it; a submission port that does not is misconfigured.
      properties.put("mail.smtp.starttls.enable", "true");
    }
    if (!smtp.authenticates()) {
      return Session.getInstance(properties);
    }
    properties.put("mail.smtp.auth", "true");
    return Session.getInstance(properties, new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(smtp.user(), smtp.password());
      }
    });
  }
}
