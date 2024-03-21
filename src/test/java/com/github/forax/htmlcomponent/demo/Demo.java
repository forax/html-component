package com.github.forax.htmlcomponent.demo;

import com.github.forax.htmlcomponent.Component;
import com.github.forax.htmlcomponent.Renderer;

import javax.xml.stream.XMLStreamException;
import java.lang.invoke.MethodHandles;
import java.util.Map;

import static com.github.forax.htmlcomponent.ComponentRegistry.REGISTRY;

public class Demo {

  record Product(Map<String, Object> attributes) implements Component {
    public Renderer render() {
      return $."""
          <row class=".product">
            <td>\{ attributes.get("name") }</td><td>\{ ((int) attributes.get("price")) * 1.20 }</td>
          </row>
          """;
    }
  }

  record App(Map<String, Object> attributes) implements Component {
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
    var lookup = MethodHandles.lookup();
    REGISTRY.register(lookup, App.class, Product.class);

    var app = REGISTRY.getComponent("App", Map.of());

    System.out.println(app.render());
  }
}
