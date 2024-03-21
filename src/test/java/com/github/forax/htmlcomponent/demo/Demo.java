package com.github.forax.htmlcomponent.demo;

import com.github.forax.htmlcomponent.Component;
import com.github.forax.htmlcomponent.ComponentRegistry;
import com.github.forax.htmlcomponent.Renderer;

import javax.xml.stream.XMLStreamException;
import java.lang.invoke.MethodHandles;
import java.util.Map;

import static java.lang.invoke.MethodHandles.lookup;

public class Demo {

  record Product(String name, int price) implements Component {
    public Renderer render() {
      return $."""
          <row class=".product">
            <td>\{ name }</td><td>\{ price * 1.20 }</td>
          </row>
          """;
    }
  }

  record App() implements Component {
    public Renderer render() {
      return $."""
          <table>
            <Product name="wood" price="\{ 10 }"/>
            <Product name="cristal" price="\{ 300 }"/>
          </table>
          """;
    }
  }

  public static void main(String[] args) throws XMLStreamException {
    var registry = new ComponentRegistry();
    registry.register(lookup(), App.class, Product.class);

    var app = registry.getComponent("App", Map.of());
    System.out.println(app.render().toString(registry));
  }
}
