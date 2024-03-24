package com.github.forax.htmlcomponent;

import com.github.forax.htmlcomponent.internal.ComponentTemplateProcessor;

import java.io.Writer;

/**
 * A component is a class able to render itself as a fragment of an XML document.
 * Technically, it's a two steps process, the component creates a {@link Renderer} which is able to emit XML events
 * and those events are later converted to a {@link Renderer#asString() String} or
 * a {@link Renderer#asWriter(Writer) Writer}.
 * <p>
 * The easy way to render a component is to declare it as a record and to use the string template processor
 * {@link #$} that converts a string template to a {@link Renderer}.
 *
 * <pre>
 * record Hello(String message) implements Component {
 *   public Renderer render() {
 *     return $."""
 *           &lt;div&gt;
 *             Hello \{message}
 *           &lt;/div&gt;
 *           """;
 *   }
 * }
 * </pre>
 *
 * @see #render()
 */
@FunctionalInterface
public interface Component {
  /**
   * Returns a renderer able to emit XML events for the current component.
   * @return a renderer able to emit XML events for the current component.
   */
  Renderer render();

  /**
   * A template processor able to convert a string template to a Renderer.
   * <p>
   * If the string template contains a reference to an XML element with a name that starts with an upper case letter,
   * the processor will look for a record implementing the interface Component named with the same name
   * inside the enclosing class calling this template processor.
   * <p>
   * This template processor will throw an {@link IllegalStateException} if the template processor is not
   * correctly formatted.
   *
   * @see Renderer
   */
  StringTemplate.Processor<Renderer, RuntimeException> $ = new ComponentTemplateProcessor();
}
