package com.github.forax.htmlcomponent.connector.spring;

import com.github.forax.htmlcomponent.Component;
import com.github.forax.htmlcomponent.Renderer;

@org.springframework.stereotype.Component
public class ProductBean implements Component {
  private String name;

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public Renderer render() {
    return $."""
          <div>\{name}</div>
          """;
  }
}