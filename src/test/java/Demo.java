import com.github.forax.htmlcomponent.Component;
import com.github.forax.htmlcomponent.ComponentRegistry;
import com.github.forax.htmlcomponent.Renderer;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;
import java.util.Map;

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
    return $."""
          <table>
            <Product name="wood" price="\{10}"/>
            <Product name="cristal" price="\{300}"/>
          </table>
          """;
  }
}

void main() {
  var registry = getRegistry(lookup(), Product.class);

  var cart = new Cart();
  System.out.println(cart.render().toString(registry));
}
