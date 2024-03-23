package com.github.forax.htmlcomponent.htmx;

import com.github.forax.htmlcomponent.Component;
import com.github.forax.htmlcomponent.ComponentRegistry;
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

import static com.github.forax.htmlcomponent.htmx.JExpress.*;
import static java.lang.System.out;
import static java.lang.invoke.MethodHandles.lookup;
import static java.nio.charset.StandardCharsets.UTF_8;

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

  private static String clean(String text) {
    return UNICODE_ESCAPE.matcher(text)
        .replaceAll(r -> new String(Character.toChars(Integer.parseInt(r.group(1), 16))));
  }

  private static List<University> extractData(Path path) throws IOException {
    var json = (List<Map<String, Object>>) JExpress.ToyJSONParser.parse(Files.readString(path));
    //out.println(json);
    return json.stream()
        .filter(data -> data.get("implantation_lib") != null)
        .map(data -> new University(
            clean((String) data.get("implantation_lib")).toLowerCase(Locale.ROOT),
            clean((String) data.get("siege_lib")),
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
    var resources = Path.of(HTMXDemo.class.getResource(".").toURI());
    var universities = extractData(resources.resolve("university.json"));

    var registry = new ComponentRegistry();
    registry.register(lookup(), UniversityListView.class, UniversityView.class);

    var app = express();
    app.use(staticFiles(resources));
    app.post("/api/university", (request, response) -> {
      var form = URLDecoder.decode(request.bodyText(), UTF_8);
      var nameFilter = form.substring(form.indexOf('=') + 1);
      response
          .type("text/xml")
          .send(new UniversityListView(universities, nameFilter).render().toString(registry));
    });
    app.listen(8080);

    out.println("application started on port 8080");
  }
}
