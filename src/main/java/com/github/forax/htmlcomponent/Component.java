package com.github.forax.htmlcomponent;

import com.github.forax.htmlcomponent.internal.ComponentTemplateProcessor;

@FunctionalInterface
public interface Component {
  Renderer render();

  StringTemplate.Processor<Renderer, RuntimeException> $ = new ComponentTemplateProcessor();
}
