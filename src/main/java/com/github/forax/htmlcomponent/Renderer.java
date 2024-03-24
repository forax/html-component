package com.github.forax.htmlcomponent;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * An abstraction that emits all the XML events to a consumer.
 * <p>
 * Implementations should override {@link #toString()} to call {@link #asString()}.
 *
 * @see Component
 */
public interface Renderer {
  /**
   * Emits XML events to a consumer.
   * @param consumer a consumer of XML events.
   */
  void emitEvents(Consumer<XMLEvent> consumer);

  /**
   * Returns a string representation of the XML events.
   * @return a string representation of the XML events.
   * <p>
   * Implementations should always call {@link #asString()}.
   */
  String toString(); // should call asString()

  /**
   * Creates a renderer able to render a stream of components by transforming each component to
   * a sequence of XML events.
   * @param stream a stream of components
   * @return a renderer able to push the XML events corresponding to the stream of components.
   */
  static Renderer from(Stream<? extends Component> stream) {
    Objects.requireNonNull(stream);
    return new Renderer() {
      @Override
      public void emitEvents(Consumer<XMLEvent> consumer) {
        stream.forEach(c -> c.render().emitEvents(consumer));
      }

      @Override
      public String toString() {
        return asString();
      }
    };
  }

  /**
   * Redirect the XML events to a text writer.
   * @param writer the writer receiving the XML events as text.
   *
   * @throws IllegalStateException if an XML event is malformed.
   */
  default void asWriter(Writer writer) {
    Objects.requireNonNull(writer);
    try {
      var eventWriter = XMLOutputFactory.newFactory().createXMLEventWriter(writer);
      try {
        emitEvents(event -> {
          try {
            eventWriter.add(event);
          } catch (XMLStreamException e) {
            throw new IllegalStateException(e);
          }
        });
        eventWriter.flush();
      } catch(Throwable t) {
        try {
          eventWriter.close();
        } catch (XMLStreamException e) {
          t.addSuppressed(e);
        }
        throw t;
      }
    } catch (XMLStreamException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Returns the concatenation of all the XML events as a string.
   * @return the concatenation of all the XML events as a string.
   */
  default String asString() {
    var writer = new StringWriter();
    asWriter(writer);
    return writer.toString();
  }
}
