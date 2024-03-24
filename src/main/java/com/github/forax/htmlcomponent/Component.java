package com.github.forax.htmlcomponent;

import com.github.forax.htmlcomponent.internal.ComponentTemplateProcessor;

import java.util.Map;

/**
 * A component is a class able to render itself as a fragment of an XML document.
 * Technically, it's a two steps process, the component creates a {@link Renderer} which is able to emit XML events
 * and those events are later converted to a String or a Reader.
 *
 * To help a component to render itself, one can use the string template {@link #$} that converts a string template
 * to a {@link Renderer}.
 *
 * @see #render()
 */
@FunctionalInterface
public interface Component {
  /**
   * Find and initialize a component from an element name and attribute values.
   * <p>
   * This interface is used by the template processor {@link #$} to find/create an instance
   * corresponding to an XML element defined inside the string template.
   * <p>
   * For example, the element {@code &lt;Message content="hello"&gt; is equivalent to calling
   * {@code getComponent("Message", Map.of("content", "hello"))}.
   */
  interface Resolver {
    /**
     * Returns a component (new or already existing) corresponding to the name {@code name} and initialized with
     * the attributes [@code attributes}.
     * @param name the name of the component
     * @param attributes the values of the attributes
     * @return a component
     */
    Component getComponent(String name, Map<String, Object> attributes);
  }

  /**
   * Returns a renderer able to emit XML events for the current component.
   * @return a renderer able to emit XML events for the current component.
   */
  Renderer render();

  /**
   * A template processor able to convert a string template to a Renderer.
   *
   * @see Renderer
   */
  StringTemplate.Processor<Renderer, RuntimeException> $ = new ComponentTemplateProcessor();
}
