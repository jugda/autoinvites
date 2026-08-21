import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The .hbs files are carried into the built jar by the {@code //FILES} directive in
 * {@link Autoinvites} and loaded from the classpath, so rendering does not depend on the
 * process working directory.
 */
public final class Templates {

  private static final Handlebars HANDLEBARS = new Handlebars(new ClassPathTemplateLoader("/templates", ".hbs"));
  private static final Map<String, Template> CACHE = new ConcurrentHashMap<>();

  private Templates() {
  }

  public static String render(String name, Map<String, Object> model) {
    var template = CACHE.computeIfAbsent(name, key -> {
      try {
        return HANDLEBARS.compile(key);
      } catch (IOException e) {
        throw new UncheckedIOException("cannot load template " + key, e);
      }
    });
    try {
      return template.apply(model);
    } catch (IOException e) {
      throw new UncheckedIOException("cannot render template " + name, e);
    }
  }
}
