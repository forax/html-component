import com.github.forax.htmlcomponent.Component;
import com.github.forax.htmlcomponent.Renderer;

import static com.github.forax.htmlcomponent.Component.$;

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
  Component cart = () ->
      $."""
      <table>
        <Product name="wood" price="\{10}"/>
        <Product name="cristal" price="\{300}"/>
      </table>
      """;
  System.out.println(cart.render());
}
