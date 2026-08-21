import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MilestonesTest {

  private static Event event(boolean external, boolean hideRegistration) {
    return new Event("e1", "Ein Talk (Marco Klaassen)", "Ein Talk", "abstract", "Marco Klaassen",
        "marco", "Darmstadt", "https://www.jug-da.de/talk/", "2026-08-20T18:30:00",
        external, hideRegistration);
  }

  private static Event event() {
    return event(false, false);
  }

  @Test
  @DisplayName("mail milestones cover the original exact day offsets")
  void coversOriginalOffsets() {
    assertEquals("announcement", Mail.due(event(), 28).orElseThrow().kind());
    assertEquals("invitation-7", Mail.due(event(), 7).orElseThrow().kind());
    assertEquals("invitation-2", Mail.due(event(), 2).orElseThrow().kind());
  }

  @Test
  @DisplayName("mail milestones catch up for a day or two after a missed run")
  void catchesUp() {
    assertEquals("announcement", Mail.due(event(), 26).orElseThrow().kind());
    assertEquals("invitation-7", Mail.due(event(), 5).orElseThrow().kind());
    assertEquals("invitation-2", Mail.due(event(), 1).orElseThrow().kind());
  }

  @Test
  @DisplayName("mail milestones never overlap or fire outside their window")
  void neverOverlaps() {
    for (var days : new int[] {40, 29, 25, 8, 4, 3, 0, -1}) {
      assertTrue(Mail.due(event(), days).isEmpty(), "unexpected mail at " + days + "d");
    }
  }

  @Test
  @DisplayName("external events never get a mail, but do get toots")
  void externalEventsAreTootOnly() {
    for (var days : new int[] {28, 7, 2}) {
      assertTrue(Mail.due(event(true, false), days).isEmpty());
    }
    assertEquals("toot-7", Toot.due(event(true, false), 7).orElseThrow().kind());
  }

  @Test
  @DisplayName("hideRegistration suppresses only the announcement")
  void hideRegistrationSuppressesAnnouncementOnly() {
    assertTrue(Mail.due(event(false, true), 28).isEmpty());
    assertEquals("invitation-7", Mail.due(event(false, true), 7).orElseThrow().kind());
  }

  @Test
  @DisplayName("toots also go out on the day itself, but only exactly then")
  void tootsOnTheDay() {
    assertEquals("toot-0", Toot.due(event(), 0).orElseThrow().kind());
    assertTrue(Toot.due(event(), -1).isEmpty());
  }

  @Test
  @DisplayName("the same-day toot says HEUTE, the others carry the date")
  void sameDayTootSaysHeute() {
    assertTrue(Toot.compose(event(), 0).startsWith("!!!HEUTE!!! "), Toot.compose(event(), 0));
    assertTrue(Toot.compose(event(), 7).startsWith("20.08.: Ein Talk"), Toot.compose(event(), 7));
  }

  @Test
  @DisplayName("the toot keeps the speaker and handle the template asks for")
  void tootCarriesSpeaker() {
    var toot = Toot.compose(event(), 7);
    assertTrue(toot.contains("(Marco Klaassen / @marco)"), toot);
    assertTrue(toot.contains("https://www.jug-da.de/talk/"), toot);
  }

  @Test
  @DisplayName("an inverted window is a programming error, caught at construction")
  void invertedWindowRejected() {
    assertThrows(IllegalArgumentException.class, () -> new Milestone("bad", "t", 10, 2));
  }
}
