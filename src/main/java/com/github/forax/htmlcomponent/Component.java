package com.github.forax.htmlcomponent;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.StringReader;
import java.io.Writer;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@FunctionalInterface
public interface Component {
  Renderer render();

  StringTemplate.Processor<Renderer, RuntimeException> $ = stringTemplate -> {
    var fragments = stringTemplate.fragments();
    var values = stringTemplate.values();
    var holes = IntStream.range(0, values.size())
        .mapToObj(i -> "$hole" + i + '$')
        .toList();
    var text = StringTemplate.of(fragments, holes).interpolate();
    var inputFactory = XMLInputFactory.newInstance();
    inputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
    XMLEventReader reader;
    try {
      reader = inputFactory.createXMLEventReader(new StringReader(text));
    } catch (XMLStreamException e) {
      throw new IllegalStateException(e);
    }
    var eventFactory = XMLEventFactory.newDefaultFactory();

    return new Renderer() {
      private record ValueAttribute(Attribute attribute, Object value) implements Attribute {
        @Override
        public QName getName() {
          return attribute.getName();
        }

        @Override
        public String getValue() {
          return String.valueOf(value);
        }

        @Override
        public String getDTDType() {
          return attribute.getDTDType();
        }

        @Override
        public boolean isSpecified() {
          return attribute.isSpecified();
        }

        @Override
        public int getEventType() {
          return attribute.getEventType();
        }

        @Override
        public Location getLocation() {
          return attribute.getLocation();
        }

        @Override
        public boolean isStartElement() {
          return attribute.isStartElement();
        }

        @Override
        public boolean isAttribute() {
          return attribute.isAttribute();
        }

        @Override
        public boolean isNamespace() {
          return attribute.isNamespace();
        }

        @Override
        public boolean isEndElement() {
          return attribute.isEndElement();
        }

        @Override
        public boolean isEntityReference() {
          return attribute.isEntityReference();
        }

        @Override
        public boolean isProcessingInstruction() {
          return attribute.isProcessingInstruction();
        }

        @Override
        public boolean isCharacters() {
          return attribute.isCharacters();
        }

        @Override
        public boolean isStartDocument() {
          return attribute.isStartDocument();
        }

        @Override
        public boolean isEndDocument() {
          return attribute.isEndDocument();
        }

        @Override
        public StartElement asStartElement() {
          return attribute.asStartElement();
        }

        @Override
        public EndElement asEndElement() {
          return attribute.asEndElement();
        }

        @Override
        public Characters asCharacters() {
          return attribute.asCharacters();
        }

        @Override
        public QName getSchemaType() {
          return attribute.getSchemaType();
        }

        @Override
        public void writeAsEncodedUnicode(Writer writer) throws XMLStreamException {
          attribute.writeAsEncodedUnicode(writer);
        }
      }

      private static final Pattern HOLE = Pattern.compile("\\$hole([0-9]+)\\$");

      private static boolean startsWithAnUpperCase(String name) {
        if (name.isEmpty()) {
          return false;
        }
        return Character.isUpperCase(name.charAt(0));
      }

      private static final class AttributeRewriterIterator implements Iterator<Attribute> {
        private final Iterator<Attribute> iterator;
        private final List<Object> values;

        private AttributeRewriterIterator(Iterator<Attribute> iterator, List<Object> values) {
          this.iterator = iterator;
          this.values = values;
        }

        @Override
        public boolean hasNext() {
          return iterator.hasNext();
        }

        @Override
        public Attribute next() {
          var attribute = iterator.next();
          var value = attribute.getValue();
          var matcher = HOLE.matcher(value);
          if (matcher.matches()) {
            var index = Integer.parseInt(matcher.group(1));
            return new ValueAttribute(attribute, values.get(index));
          }
          return attribute;
        }
      }

      private static Map<String, Object> asAttributeMap(Iterator<Attribute> iterator) {
        var map = new LinkedHashMap<String, Object>();
        while (iterator.hasNext()) {
          var attribute = iterator.next();
          var name = attribute.getName().getLocalPart();
          map.put(name, switch (attribute) {
            case ValueAttribute valueAttribute -> valueAttribute.value();
            case Attribute _ -> attribute.getValue();
          });
        }
        return Collections.unmodifiableMap(map);
      }

      @Override
      public void advance(ComponentRegistry registry, Consumer<XMLEvent> consumer) {
        while(reader.hasNext()) {
          XMLEvent event;
          try {
            event = reader.nextEvent();
          } catch (XMLStreamException e) {
            throw new NoSuchElementException(e);
          }
          switch (event) {
            case StartDocument _, EndDocument _ -> {}
            case Characters characters -> {
              var text = HOLE.matcher(characters.getData())
                  .replaceAll(result -> {
                    var index = Integer.parseInt(result.group(1));
                    return String.valueOf(values.get(index));
                  });
              consumer.accept(eventFactory.createCharacters(text));
            }
            case StartElement startElement -> {
              var name = startElement.getName().getLocalPart();
              var attributeIterator = new AttributeRewriterIterator(startElement.getAttributes(), values);
              if (startsWithAnUpperCase(name)) {
                var component = registry.getComponent(name, asAttributeMap(attributeIterator));
                component.render().advance(registry, consumer);
              } else {
                var newEvent = eventFactory.createStartElement(startElement.getName(), attributeIterator, startElement.getNamespaces());
                consumer.accept(newEvent);
              }
            }
            case EndElement endElement when startsWithAnUpperCase(endElement.getName().getLocalPart()) -> {}
            default -> consumer.accept(event);
          }
        }
      }
    };
  };
}
