package com.github.forax.htmlcomponent.internal;

import com.github.forax.htmlcomponent.Component;
import com.github.forax.htmlcomponent.Renderer;

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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public final class ComponentTemplateProcessor implements StringTemplate.Processor<Renderer, RuntimeException> {
  private static final Pattern HOLE = Pattern.compile("\\$hole([0-9]+)\\$");

  private static boolean startsWithAnUpperCase(String name) {
    if (name.isEmpty()) {
      return false;
    }
    return Character.isUpperCase(name.charAt(0));
  }

  private record AttributeRewriterIterator(Iterator<Attribute> iterator,
                                           List<Object> values) implements Iterator<Attribute> {
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

  private static void emitCharacters(Characters characters, List<Object> values, XMLEventFactory eventFactory, Component.Resolver resolver, Consumer<XMLEvent> consumer) {
    var data = characters.getData();
    var matcher = HOLE.matcher(data);
    if (!matcher.find()) {
      consumer.accept(characters);
      return;
    }
    var builder = new StringBuilder();
    var current = 0;
    for (; ; ) {
      builder.append(data, current, matcher.start());
      var index = Integer.parseInt(matcher.group(1));
      switch (values.get(index)) {
        case Renderer renderer -> {
          if (!builder.isEmpty()) {
            eventFactory.setLocation(null);  // TODO
            consumer.accept(eventFactory.createCharacters(builder.toString()));
            builder.setLength(0);
          }
          renderer.emitEvents(resolver, consumer);
        }
        case null -> builder.append("null");
        case Object o -> builder.append(o);
      }
      current = matcher.end();
      if (!matcher.find()) {
        builder.append(data, current, data.length());
        eventFactory.setLocation(null);  // TODO
        consumer.accept(eventFactory.createCharacters(builder.toString()));
        break;
      }
    }
  }

  private static void emitStartElement(StartElement startElement, List<Object> values, XMLEventFactory eventFactory, Component.Resolver resolver, Consumer<XMLEvent> consumer) {
    var name = startElement.getName().getLocalPart();
    var attributeIterator = new AttributeRewriterIterator(startElement.getAttributes(), values);
    if (startsWithAnUpperCase(name)) {
      var component = resolver.getComponent(name, asAttributeMap(attributeIterator));
      component.render().emitEvents(resolver, consumer);
      return;
    }
    eventFactory.setLocation(startElement.getLocation());
    var newEvent = eventFactory.createStartElement(startElement.getName(), attributeIterator, startElement.getNamespaces());
    consumer.accept(newEvent);
  }

  @Override
  public Renderer process(StringTemplate stringTemplate) throws RuntimeException {
    Objects.requireNonNull(stringTemplate);
    var fragments = stringTemplate.fragments();
    var values = stringTemplate.values();
    var holes = IntStream.range(0, values.size())
        .mapToObj(i -> "$hole" + i + '$')
        .toList();
    var text = StringTemplate.of(fragments, holes).interpolate();
    var inputFactory = XMLInputFactory.newInstance();
    inputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
    XMLEventReader reader;  // TODO, create the DOM and cache it
    try {
      reader = inputFactory.createXMLEventReader(new StringReader(text));
    } catch (XMLStreamException e) {
      throw new IllegalStateException(e);
    }

    return (resolver, consumer) -> {
      var eventFactory = XMLEventFactory.newDefaultFactory();
      while(reader.hasNext()) {
        XMLEvent event;
        try {
          event = reader.nextEvent();
        } catch (XMLStreamException e) {
          throw new NoSuchElementException("error while parsing\n" + text, e);
        }
        switch (event) {
          case StartDocument _, EndDocument _ -> {}
          case Characters characters -> emitCharacters(characters, values, eventFactory, resolver, consumer);
          case StartElement startElement -> emitStartElement(startElement, values, eventFactory, resolver, consumer);
          case EndElement endElement when startsWithAnUpperCase(endElement.getName().getLocalPart()) -> {}
          default -> consumer.accept(event);
        }
      }
    };
  }
}
