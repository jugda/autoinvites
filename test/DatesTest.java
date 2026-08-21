import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatesTest {

  private static final String START = "2026-08-20T18:30:00";

  @Test
  @DisplayName("formats German dates exactly as the previous implementations did")
  void formatsGermanDates() {
    var at = Dates.parseStart(START).orElseThrow();
    assertEquals("20.08.2026", Dates.shortDate(at));
    assertEquals("Donnerstag, 20. August 2026", Dates.longDate(at));
    assertEquals("18:30", Dates.time(at));
    assertEquals("20.08.", Dates.dayMonth(at));
  }

  @Test
  @DisplayName("keeps the feed wall-clock, which carries no offset")
  void keepsWallClock() {
    assertEquals(18, Dates.parseStart(START).orElseThrow().getHour());
    assertEquals(30, Dates.parseStart(START).orElseThrow().getMinute());
  }

  @Test
  @DisplayName("rejects timestamps it cannot read")
  void rejectsUnparseable() {
    assertFalse(Dates.parseStart(null).isPresent());
    assertFalse(Dates.parseStart("").isPresent());
    assertFalse(Dates.parseStart("not a date").isPresent());
    assertFalse(Dates.daysUntil("nonsense", LocalDate.of(2026, 8, 1)).isPresent());
  }

  @Test
  @DisplayName("accepts a date without a time")
  void acceptsDateOnly() {
    assertTrue(Dates.parseStart("2026-08-20").isPresent());
    assertEquals(0, Dates.parseStart("2026-08-20").orElseThrow().getHour());
  }

  @Test
  @DisplayName("counts whole days to the event")
  void countsWholeDays() {
    var today = LocalDate.of(2026, 8, 1);
    assertEquals(0, Dates.daysUntil("2026-08-01T18:30:00", today).getAsInt());
    assertEquals(2, Dates.daysUntil("2026-08-03T18:30:00", today).getAsInt());
    assertEquals(28, Dates.daysUntil("2026-08-29T18:30:00", today).getAsInt());
    assertEquals(-1, Dates.daysUntil("2026-07-31T18:30:00", today).getAsInt());
  }

  @Test
  @DisplayName("is not thrown off by the DST switch")
  void survivesDaylightSaving() {
    // Germany leaves CEST on 2026-10-25; counting in wall-clock milliseconds is off by one here.
    assertEquals(7, Dates.daysUntil("2026-10-30T19:00:00", LocalDate.of(2026, 10, 23)).getAsInt());
  }
}
