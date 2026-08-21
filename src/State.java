import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Which invitations have already gone out, keyed {@code uid:kind}.
 *
 * <p>The original design matched day offsets exactly, so a single missed run silently
 * skipped an invitation and a re-run sent it twice. GitHub Actions schedules are
 * best-effort - GitHub documents that queued jobs may be dropped - so what was sent is
 * recorded here instead, and each milestone catches up over a small window.
 */
public final class State {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final int KEEP_UNKNOWN_FOR_DAYS = 60;

  /**
   * Jackson's default puts a space before the colon. This file is committed on every send,
   * so it is worth writing conventional JSON that diffs cleanly for the next reader.
   */
  private static final DefaultPrettyPrinter PRINTER = new DefaultPrettyPrinter()
      .withSeparators(Separators.createDefaultInstance()
          .withObjectFieldValueSpacing(Separators.Spacing.AFTER));

  private final Path file;
  private final TreeMap<String, String> sent;

  private State(Path file, TreeMap<String, String> sent) {
    this.file = file;
    this.sent = sent;
  }

  public static String key(String uid, String kind) {
    return uid + ":" + kind;
  }

  public static State load(Path file) {
    var sent = new TreeMap<String, String>();
    if (Files.exists(file)) {
      try {
        var root = MAPPER.readTree(Files.readString(file));
        var entries = root.get("sent");
        if (entries != null) {
          entries.properties().forEach(e -> sent.put(e.getKey(), e.getValue().asText()));
        }
      } catch (IOException e) {
        throw new UncheckedIOException("cannot read " + file, e);
      }
    }
    return new State(file, sent);
  }

  public boolean alreadySent(String key) {
    return sent.containsKey(key);
  }

  public void record(String key, LocalDate on) {
    sent.put(key, on.toString());
  }

  public int size() {
    return sent.size();
  }

  public Map<String, String> entries() {
    return Map.copyOf(sent);
  }

  /**
   * Drops entries whose event has left the feed <em>and</em> whose send is old enough to be
   * irrelevant. Both conditions, so a truncated or failed feed fetch can never wipe the
   * state and trigger a storm of re-sends.
   */
  public void prune(Set<String> liveUids, LocalDate today) {
    var cutoff = today.minusDays(KEEP_UNKNOWN_FOR_DAYS).toString();
    sent.entrySet().removeIf(entry -> {
      var uid = entry.getKey().substring(0, entry.getKey().lastIndexOf(':'));
      return !liveUids.contains(uid) && entry.getValue().compareTo(cutoff) < 0;
    });
  }

  public void save() {
    var root = MAPPER.createObjectNode();
    root.put("version", 1);
    ObjectNode entries = root.putObject("sent");
    sent.forEach(entries::put);
    try {
      Files.createDirectories(file.toAbsolutePath().getParent());
      Files.writeString(file, MAPPER.writer(PRINTER).writeValueAsString(root) + "\n");
    } catch (IOException e) {
      throw new UncheckedIOException("cannot write " + file, e);
    }
  }
}
