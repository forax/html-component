import com.github.forax.htmlcomponent.Component;
import com.github.forax.htmlcomponent.ComponentRegistry;
import com.github.forax.htmlcomponent.Renderer;

import java.util.List;

import static com.github.forax.htmlcomponent.ComponentRegistry.getRegistry;
import static java.lang.invoke.MethodHandles.lookup;

record Product(String name, int price) implements Component {
  public Renderer render() {
    return $."""
          <tr class=".product">
            <td>\{name}</td><td>\{price * 1.20}</td>
          </tr>
          """;
  }
}

record Cart() implements Component {
  public Renderer render() {
    var products = List.of(new Product("wood", 10), new Product("jade", 50));
    return $."""
        <table>
        \{ Renderer.from(products.stream()) }
        </table>
        """;
  }
}

void main() {
  var registry = getRegistry(lookup());

  var cart = new Cart();
  System.out.println(cart.render().toString(registry));
}
