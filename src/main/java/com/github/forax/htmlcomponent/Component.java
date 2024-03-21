package com.github.forax.htmlcomponent;

import com.github.forax.htmlcomponent.internal.ComponentTemplateProcessor;

import java.util.Map;

@FunctionalInterface
public interface Component {
  interface Resolver {
    Component getComponent(String name, Map<String, Object> attributes);
  }

  Renderer render();

  StringTemplate.Processor<Renderer, RuntimeException> $ = new ComponentTemplateProcessor();
}
