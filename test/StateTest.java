import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StateTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 8, 1);

  private static State seeded(Path file, String key, String sentOn) {
    var state = State.load(file);
    state.record(key, LocalDate.parse(sentOn));
    return state;
  }

  @Test
  @DisplayName("survives a save/load round trip")
  void roundTrips(@TempDir Path dir) {
    var file = dir.resolve("state/sent.json");
    seeded(file, State.key("e1", "toot-7"), "2026-07-30").save();

    var reloaded = State.load(file);
    assertTrue(reloaded.alreadySent(State.key("e1", "toot-7")));
    assertFalse(reloaded.alreadySent(State.key("e1", "toot-2")));
  }

  @Test
  @DisplayName("writes a stable, diffable file so the workflow commit stays readable")
  void writesStableFile(@TempDir Path dir) throws Exception {
    var file = dir.resolve("sent.json");
    var state = State.load(file);
    state.record(State.key("z", "toot-0"), TODAY);
    state.record(State.key("a", "toot-7"), TODAY);
    state.save();

    var written = Files.readString(file);
    assertTrue(written.indexOf("\"a:toot-7\"") < written.indexOf("\"z:toot-0\""), "keys should be sorted");
    // Conventional JSON spacing, not Jackson's default "version" : 1 - this file is committed.
    assertTrue(written.contains("\"version\": 1"), written);
    assertTrue(written.endsWith("\n"), "should end with a newline");

    State.load(file).save();
    assertEquals(written, Files.readString(file), "saving unchanged state should not churn the file");
  }

  @Test
  @DisplayName("missing file loads as empty rather than exploding")
  void missingFileIsEmpty(@TempDir Path dir) {
    assertEquals(0, State.load(dir.resolve("nope.json")).size());
  }

  @Test
  @DisplayName("pruning keeps anything still in the feed")
  void pruneKeepsLive(@TempDir Path dir) {
    var state = seeded(dir.resolve("s.json"), State.key("e1", "toot-7"), "2020-01-01");
    state.prune(Set.of("e1"), TODAY);
    assertTrue(state.alreadySent(State.key("e1", "toot-7")));
  }

  @Test
  @DisplayName("pruning keeps recent sends even once the event leaves the feed")
  void pruneKeepsRecent(@TempDir Path dir) {
    var state = seeded(dir.resolve("s.json"), State.key("gone", "toot-7"), "2026-07-20");
    state.prune(Set.of(), TODAY);
    assertTrue(state.alreadySent(State.key("gone", "toot-7")));
  }

  @Test
  @DisplayName("pruning drops only old sends for events that are gone")
  void pruneDropsOldAndGone(@TempDir Path dir) {
    var state = seeded(dir.resolve("s.json"), State.key("gone", "toot-7"), "2026-01-01");
    state.prune(Set.of(), TODAY);
    assertEquals(0, state.size());
  }

  @Test
  @DisplayName("an empty feed cannot wipe recent state")
  void emptyFeedCannotWipeState(@TempDir Path dir) {
    var state = State.load(dir.resolve("s.json"));
    state.record(State.key("e1", "toot-7"), LocalDate.of(2026, 7, 30));
    state.record(State.key("e2", "invitation-2"), LocalDate.of(2026, 7, 25));
    state.prune(Set.of(), TODAY);
    assertEquals(2, state.size());
  }
}
