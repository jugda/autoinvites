/**
 * One scheduled send for an event, firing anywhere in the inclusive window
 * {@code [from, to]} days before it.
 *
 * <p>A window rather than an exact day is what lets a missed run catch up the next day.
 * {@link State} is what keeps it to one send regardless.
 */
public record Milestone(String kind, String template, int from, int to) {

  public Milestone {
    if (from > to) {
      throw new IllegalArgumentException("milestone " + kind + " has an inverted window");
    }
  }

  public boolean covers(int daysUntil) {
    return daysUntil >= from && daysUntil <= to;
  }
}
