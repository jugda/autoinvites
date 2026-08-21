import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Toot {

  /**
   * The same catch-up windows as the mails, plus a post on the day itself. {@code toot-0}
   * stays an exact match on purpose: it renders "!!!HEUTE!!!", so it must not fire late.
   */
  private static final List<Milestone> MILESTONES = List.of(
      new Milestone("toot-28", "mastodon_invitation", 26, 28),
      new Milestone("toot-7", "mastodon_invitation", 5, 7),
      new Milestone("toot-2", "mastodon_invitation", 1, 2),
      new Milestone("toot-0", "mastodon_invitation", 0, 0));

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final HttpClient CLIENT =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

  private Toot() {
  }

  public static Optional<Milestone> due(Event event, int daysUntil) {
    return MILESTONES.stream().filter(milestone -> milestone.covers(daysUntil)).findFirst();
  }

  public static String compose(Event event, int daysUntil) {
    var day = daysUntil == 0
        ? "!!!HEUTE!!! "
        : Dates.dayMonth(Dates.parseStart(event.start()).orElseThrow());
    return Templates.render("mastodon_invitation", event.model(Map.of("day", day)));
  }

  public static String send(Event event, int daysUntil, Config.Mastodon config) throws Exception {
    var payload = MAPPER.writeValueAsString(Map.of(
        "status", compose(event, daysUntil),
        "visibility", "public",
        "language", "de"));

    var request = HttpRequest.newBuilder(URI.create(config.url().replaceAll("/+$", "") + "/api/v1/statuses"))
        .header("Authorization", "Bearer " + config.accessToken())
        .header("Content-Type", "application/json")
        .timeout(Duration.ofSeconds(30))
        .POST(HttpRequest.BodyPublishers.ofString(payload))
        .build();

    var response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() / 100 != 2) {
      throw new IllegalStateException("Mastodon responded " + response.statusCode() + ": " + response.body());
    }
    return MAPPER.readTree(response.body()).path("url").asText();
  }
}
