import com.github.forax.htmlcomponent.Component;
import com.github.forax.htmlcomponent.Renderer;

import java.util.List;

record Product(String name, int price) implements Component {
  public Renderer render() {
    return $."""
          <tr class=".product">
            <td>\{name}</td><td>\{price * 1.20}</td>
          </tr>
          """;
  }
}

record Cart(List<Product> products) implements Component {
  public Renderer render() {
    return $."""
        <table>
        \{ Renderer.from(products.stream()) }
        </table>
        """;
  }
}

void main() {
  var products = List.of(new Product("wood", 10), new Product("jade", 50));
  var cart = new Cart(products);
  System.out.println(cart.render());
}
