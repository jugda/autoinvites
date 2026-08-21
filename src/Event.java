import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;

/** One entry of the events JSON feed. Unknown fields are ignored so the feed can grow. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Event(
    String uid,
    String summary,
    String title,
    String description,
    String speaker,
    String twitter,
    String location,
    String url,
    String start,
    boolean externalEvent,
    boolean hideRegistration) {

  /**
   * The Handlebars model. A copy every time, with the derived fields layered on top - the
   * templates are rendered from a map so the event itself is never mutated.
   */
  public Map<String, Object> model(Map<String, String> derived) {
    var model = new HashMap<String, Object>();
    model.put("uid", uid);
    model.put("summary", summary);
    model.put("title", title);
    model.put("description", description);
    model.put("speaker", speaker);
    model.put("twitter", twitter);
    model.put("location", location);
    model.put("url", url);
    model.putAll(derived);
    return model;
  }
}
