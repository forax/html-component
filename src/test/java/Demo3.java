import com.github.forax.htmlcomponent.Component;
import com.github.forax.htmlcomponent.ComponentRegistry;
import com.github.forax.htmlcomponent.Renderer;

import java.util.Map;

import static com.github.forax.htmlcomponent.Component.$;
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

void main() {
  var registry = new ComponentRegistry();
  registry.register(lookup(), Product.class);
  registry.registerFactory("Cart", _ -> () ->
      $."""
      <table>
        <Product name="wood" price="\{10}"/>
        <Product name="cristal" price="\{300}"/>
      </table>
      """);
  var cart = registry.getComponent("Cart", Map.of());
  System.out.println(cart.render().toString(registry));
}
