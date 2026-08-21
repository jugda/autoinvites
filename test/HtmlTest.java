import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlTest {

  @Test
  @DisplayName("keeps link targets, which a tag-stripping converter silently loses")
  void keepsLinkTargets() {
    var text = Html.toPlainText("<p>Termine per <a href=\"https://www.jug-da.de/events.ics\">iCal abonnieren</a></p>");
    assertTrue(text.contains("iCal abonnieren"), text);
    assertTrue(text.contains("https://www.jug-da.de/events.ics"), text);
  }

  @Test
  @DisplayName("does not print the URL twice when the link text already is the URL")
  void doesNotDuplicateUrls() {
    var url = "https://www.jug-da.de/2026/09/Quarkus/";
    var text = Html.toPlainText("<p><a href=\"" + url + "\">" + url + "</a></p>");
    assertEquals(1, text.split("jug-da\\.de", -1).length - 1, text);
  }

  @Test
  @DisplayName("turns block elements and breaks into line breaks")
  void breaksBlocks() {
    var text = Html.toPlainText("<p>Erste Zeile</p><p>Zweite<br/>Dritte</p>");
    assertTrue(text.contains("Erste Zeile\n"), text);
    assertTrue(text.contains("Zweite\nDritte"), text);
  }

  @Test
  @DisplayName("emits no markup")
  void emitsNoMarkup() {
    var text = Html.toPlainText("<p>Hallo <b>Welt</b> <i>und</i> so</p>");
    assertFalse(text.contains("<"), text);
    assertTrue(text.contains("Hallo Welt und so"), text);
  }

  @Test
  @DisplayName("keeps German umlauts intact")
  void keepsUmlauts() {
    assertTrue(Html.toPlainText("<p>Ank&uuml;ndigung f&uuml;r Gr&uuml;&szlig;e</p>").contains("Ankündigung für Grüße"));
  }
}
