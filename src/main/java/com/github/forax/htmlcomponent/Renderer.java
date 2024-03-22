package com.github.forax.htmlcomponent;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

@FunctionalInterface
public interface Renderer {
  void emitEvents(Component.Resolver resolver, Consumer<XMLEvent> consumer);

  static Renderer from(Stream<? extends Component> stream) {
    Objects.requireNonNull(stream);
    return (resolver, consumer) -> stream.forEach(c -> c.render().emitEvents(resolver, consumer));
  }

  default void toWriter(Component.Resolver resolver, Writer writer) {
    Objects.requireNonNull(resolver);
    Objects.requireNonNull(writer);
    try {
      var eventWriter = XMLOutputFactory.newFactory().createXMLEventWriter(writer);
      try {
        emitEvents(resolver, event -> {
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

  default String toString(Component.Resolver resolver) {
    Objects.requireNonNull(resolver);
    var writer = new StringWriter();
    toWriter(resolver, writer);
    return writer.toString();
  }
}
