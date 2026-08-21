import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Feed timestamps are naive Berlin wall-clock ("2026-08-20T18:30:00", no offset). That is
 * precisely what LocalDateTime models, so unlike the JavaScript version none of this needs
 * anchoring tricks to stop the host timezone leaking into the rendered output.
 */
public final class Dates {

  private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

  private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN);
  private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("EEEE, dd. MMMM yyyy", Locale.GERMAN);
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN);
  private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd.MM.", Locale.GERMAN);

  private Dates() {
  }

  public static Optional<LocalDateTime> parseStart(String value) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    var trimmed = value.trim().replace(' ', 'T');
    try {
      return Optional.of(LocalDateTime.parse(trimmed));
    } catch (DateTimeParseException withTime) {
      try {
        return Optional.of(LocalDate.parse(trimmed).atStartOfDay());
      } catch (DateTimeParseException dateOnly) {
        return Optional.empty();
      }
    }
  }

  public static LocalDate today() {
    return LocalDate.now(BERLIN);
  }

  /** Whole days from {@code today} until the event's calendar day; empty if unparseable. */
  public static OptionalInt daysUntil(String start, LocalDate today) {
    return parseStart(start)
        .map(at -> OptionalInt.of((int) ChronoUnit.DAYS.between(today, at.toLocalDate())))
        .orElseGet(OptionalInt::empty);
  }

  public static String shortDate(LocalDateTime at) {
    return SHORT_DATE.format(at);
  }

  public static String longDate(LocalDateTime at) {
    return LONG_DATE.format(at);
  }

  public static String time(LocalDateTime at) {
    return TIME.format(at);
  }

  public static String dayMonth(LocalDateTime at) {
    return DAY_MONTH.format(at);
  }
}
