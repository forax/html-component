package com.github.forax.htmlcomponent;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.StringWriter;
import java.io.Writer;
import java.util.function.Consumer;

public interface Renderer {
  void advance(ComponentRegistry registry, Consumer<XMLEvent> consumer);

  default void toWriter(ComponentRegistry registry, Writer writer) {
    try {
      var eventWriter = XMLOutputFactory.newFactory().createXMLEventWriter(writer);
      try {
        advance(registry, event -> {
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

  default String toString(ComponentRegistry registry) {
    var writer = new StringWriter();
    toWriter(registry, writer);
    return writer.toString();
  }
}
