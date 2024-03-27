import com.github.forax.htmlcomponent.Component;
import com.github.forax.htmlcomponent.Renderer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import static java.lang.System.out;
import static java.nio.charset.StandardCharsets.UTF_8;

// java --enable-preview --source 22 --class-path /Users/forax/git/html-component/target/html-component-1.0-SNAPSHOT.jar HTMXDemo.java
public class HTMXDemo {
  record University(String name, String headquarter, String creation, String wikiurl) {
    University {
      Objects.requireNonNull(name);
      Objects.requireNonNull(headquarter);
      Objects.requireNonNull(creation);
      Objects.requireNonNull(wikiurl);
    }
  }

  private static final Pattern UNICODE_ESCAPE = Pattern.compile("\\\\u([0-9a-fA-F]{4})");

  private static String cleanData(String text) {
    return UNICODE_ESCAPE.matcher(text)
        .replaceAll(r -> new String(Character.toChars(Integer.parseInt(r.group(1), 16))));
  }

  private static List<University> extractData(Path path) throws IOException {
    @SuppressWarnings("unchecked")
    var json = (List<Map<String, Object>>) JExpress.ToyJSONParser.parse(Files.readString(path));
    //out.println(json);
    return json.stream()
        .filter(data -> data.get("implantation_lib") != null)
        .map(data -> new University(
            cleanData((String) data.get("implantation_lib")).toLowerCase(Locale.ROOT),
            cleanData((String) data.get("siege_lib")),
            (String) data.get("date_ouverture"),
            (String) data.get("element_wikidata")
        ))
        .toList();
  }

  record UniversityView(String name, String headquarter, String creation, String wikiurl) implements Component {
    public Renderer render() {
      return $."""
          <tr>
            <td><a href="\{wikiurl}">\{name}</a></td>
            <td>\{headquarter}</td>
            <td>\{creation}</td>
          </tr>
          """;
    }
  }

  record UniversityListView(List<University> universities, String nameFilter) implements Component {
    public Renderer render() {
      return $."""
          <table>
           <tr>
            <th>name</th><th>headquarter</th><th>creation date</th>
           </tr>
            \{
               Renderer.from(universities.stream()
                   .map(u -> new UniversityView(u.name, u.headquarter, u.creation, u.wikiurl))
                   .filter(u -> u.name.contains(nameFilter)))
             }
          </table>
          """;
    }
  }

  void main() throws URISyntaxException, IOException {
    var root = Path.of(HTMXDemo.class.getResource(".").toURI());
    var resources = root.resolve("../resources");
    if (!Files.isDirectory(resources)) {
      resources = root;
    }
    var universities = extractData(resources.resolve("university.json"));

    var app = JExpress.express();
    app.use(JExpress.staticFiles(resources));
    app.post("/api/university", (request, response) -> {
      var formData = URLDecoder.decode(request.bodyText(), UTF_8);
      var nameFilter = formData.substring(formData.indexOf('=') + 1);
      var view = new UniversityListView(universities, nameFilter);
      response
          .type("text/xml")
          .send(view.render().asString());
    });
    app.listen(8080);

    out.println("application started on port 8080");
  }
}
