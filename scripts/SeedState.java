///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//SOURCES ../src/Autoinvites.java

import java.time.LocalDate;
import java.util.List;

/**
 * One-off cutover helper.
 *
 * <p>The original AWS Lambda fired on exact day offsets (mail 28/7/2, toot 28/7/2/0) and kept
 * no record of what it had done. This implementation uses catch-up windows, so starting from
 * empty state would re-send anything currently inside one. Marking every milestone whose
 * trigger day has already passed as "sent by the old system" makes the cutover silent.
 *
 * <pre>EVENTS_URL=https://www.jug-da.de/events.json jbang scripts/SeedState.java</pre>
 */
public class SeedState {

  /** What the old implementation did, kept deliberately separate from the current windows. */
  record Trigger(String kind, int day, boolean mail) {
  }

  private static final List<Trigger> OLD_TRIGGERS = List.of(
      new Trigger("announcement", 28, true),
      new Trigger("invitation-7", 7, true),
      new Trigger("invitation-2", 2, true),
      new Trigger("toot-28", 28, false),
      new Trigger("toot-7", 7, false),
      new Trigger("toot-2", 2, false),
      new Trigger("toot-0", 0, false));

  public static void main(String[] args) throws Exception {
    var config = Config.load(System.getenv());
    var today = Dates.today();
    var events = Autoinvites.fetchEvents(config.eventsUrl());
    var state = State.load(config.stateFile());
    var added = 0;

    for (var event : events) {
      var days = Dates.daysUntil(event.start(), today);
      if (days.isEmpty()) {
        continue;
      }
      for (var trigger : OLD_TRIGGERS) {
        if (days.getAsInt() > trigger.day()) {
          continue; // trigger day still ahead; let the normal run send it
        }
        if (trigger.mail() && event.externalEvent()) {
          continue;
        }
        if (trigger.kind().equals("announcement") && event.hideRegistration()) {
          continue;
        }
        var key = State.key(event.uid(), trigger.kind());
        if (state.alreadySent(key)) {
          continue;
        }
        state.record(key, today);
        added++;
        System.out.println("seeded " + key + " (event is " + days.getAsInt() + "d out)");
      }
    }

    state.save();
    System.out.println("seeded " + added + " entries");
  }
}
