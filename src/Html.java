import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import java.util.Set;

/**
 * The text/plain half of the mail.
 *
 * <p>Links have to survive: a converter that merely strips tags silently drops every URL,
 * which is what the original {@code strip} dependency did and why the plain-text part used
 * to be near useless.
 */
public final class Html {

  private static final Set<String> BLOCKS =
      Set.of("p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "li", "tr", "blockquote");
  private static final int WRAP = 78;

  private Html() {
  }

  public static String toPlainText(String html) {
    var out = new StringBuilder();
    NodeTraversor.traverse(new NodeVisitor() {
      @Override
      public void head(Node node, int depth) {
        if (node instanceof TextNode text) {
          out.append(text.text());
        } else if (node instanceof Element element) {
          var tag = element.tagName();
          if (tag.equals("br") || BLOCKS.contains(tag)) {
            newline(out);
          }
        }
      }

      @Override
      public void tail(Node node, int depth) {
        if (!(node instanceof Element element)) {
          return;
        }
        var tag = element.tagName();
        if (tag.equals("a")) {
          var href = element.absUrl("href").isBlank() ? element.attr("href") : element.absUrl("href");
          // Printing the URL twice helps nobody when the text already is the URL.
          if (!href.isBlank() && !href.equals(element.text().trim())) {
            out.append(" [").append(href).append(']');
          }
        } else if (BLOCKS.contains(tag)) {
          newline(out);
          out.append('\n');
        }
      }
    }, Jsoup.parse(html).body());

    return wrap(out.toString().replaceAll("[ \t]+", " ").replaceAll("\n{3,}", "\n\n").strip());
  }

  private static void newline(StringBuilder out) {
    if (!out.isEmpty() && out.charAt(out.length() - 1) != '\n') {
      out.append('\n');
    }
  }

  private static String wrap(String text) {
    var wrapped = new StringBuilder();
    for (var line : text.split("\n", -1)) {
      if (line.length() <= WRAP) {
        wrapped.append(line).append('\n');
        continue;
      }
      var current = new StringBuilder();
      for (var word : line.strip().split(" ")) {
        if (!current.isEmpty() && current.length() + 1 + word.length() > WRAP) {
          wrapped.append(current).append('\n');
          current.setLength(0);
        }
        current.append(current.isEmpty() ? "" : " ").append(word);
      }
      wrapped.append(current).append('\n');
    }
    return wrapped.toString().strip();
  }
}
