///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS com.fasterxml.jackson.core:jackson-databind:2.22.2
//DEPS com.github.jknack:handlebars:4.5.4
//DEPS org.eclipse.angus:angus-mail:2.0.5
//DEPS org.jsoup:jsoup:1.23.1
//DEPS org.slf4j:slf4j-nop:2.0.18
//FILES templates/=../templates/
//SOURCES Config.java Dates.java Event.java Html.java Mail.java Milestone.java State.java Templates.java Toot.java

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Autoinvites {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static void main(String[] args) throws Exception {
    var config = Config.load(System.getenv());
    var today = Dates.today();

    var events = fetchEvents(config.eventsUrl());
    var state = State.load(config.stateFile());
    var failures = new ArrayList<String>();
    var delivered = 0;

    System.out.println(events.size() + " events in feed"
        + (config.dryRun() ? " (DRY_RUN: nothing will be sent)" : ""));
    if (config.mastodon() == null) {
      System.out.println("MASTO_URL/MASTO_TOKEN unset - not tooting");
    }

    for (var event : events) {
      var maybeDays = Dates.daysUntil(event.start(), today);
      if (maybeDays.isEmpty()) {
        continue;
      }
      var days = maybeDays.getAsInt();

      var jobs = new ArrayList<Job>();
      Mail.due(event, days).ifPresent(milestone ->
          jobs.add(new Job(milestone.kind(), () -> Mail.send(event, milestone, config.mail()))));
      if (config.mastodon() != null) {
        Toot.due(event, days).ifPresent(milestone ->
            jobs.add(new Job(milestone.kind(), () -> Toot.send(event, days, config.mastodon()))));
      }

      for (var job : jobs) {
        var key = State.key(event.uid(), job.kind());
        if (state.alreadySent(key)) {
          continue;
        }
        if (config.dryRun()) {
          System.out.println("would send " + job.kind() + " for " + event.uid() + " (in " + days + "d)");
          continue;
        }
        try {
          var reference = job.action().run();
          // Recorded only once the send actually returned, so a failure retries tomorrow.
          state.record(key, today);
          delivered++;
          System.out.println("sent " + job.kind() + " for " + event.uid() + " (in " + days + "d) -> " + reference);
        } catch (Exception e) {
          failures.add(job.kind() + " for " + event.uid() + ": " + e.getMessage());
          System.err.println("FAILED " + job.kind() + " for " + event.uid() + ": " + e);
        }
      }
    }

    if (!config.dryRun()) {
      state.prune(events.stream().map(Event::uid).collect(Collectors.toSet()), today);
      state.save();
    }

    System.out.println("done: " + delivered + " sent, " + failures.size() + " failed");
    if (!failures.isEmpty()) {
      throw new IllegalStateException(failures.size() + " send(s) failed:\n  " + String.join("\n  ", failures));
    }
  }

  interface Action {
    String run() throws Exception;
  }

  record Job(String kind, Action action) {
  }

  static List<Event> fetchEvents(String url) throws Exception {
    var request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).GET().build();
    var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() / 100 != 2) {
      throw new IllegalStateException("events feed " + url + " responded " + response.statusCode());
    }
    return MAPPER.readerForListOf(Event.class).readValue(response.body());
  }
}
