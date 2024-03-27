package com.github.forax.htmlcomponent;

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
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static java.lang.StackWalker.Option.DROP_METHOD_INFO;
import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;
import static java.lang.invoke.MethodType.methodType;
import static java.util.stream.Collectors.toMap;

/**
 * A template processor that takes a template string formatted in XML and create the corresponding {@link Renderer}.
 * see Component
 */
final class ComponentTemplateProcessor implements StringTemplate.Processor<Renderer, RuntimeException> {
  private static final Pattern HOLE = Pattern.compile("\\$hole([0-9]+)\\$");

  private static final String PACKAGE_NAME = ComponentTemplateProcessor.class.getPackageName();
  private static final StackWalker STACK_WALKER =
      StackWalker.getInstance(Set.of(RETAIN_CLASS_REFERENCE, DROP_METHOD_INFO));
  private static final ClassValue<BiFunction<String, Map<String, Object>, Component>> FACTORY_CACHE =
      new ClassValue<>() {
        @Override
        protected BiFunction<String, Map<String, Object>, Component> computeValue(Class<?> nestHost) {
          var lookup = MethodHandles.lookup();
          Lookup nestHostLookup;
          try {
            nestHostLookup = MethodHandles.privateLookupIn(nestHost, lookup);
          } catch (IllegalAccessException e) {
            throw (IllegalAccessError) new IllegalAccessError("Package of class " + nestHost.getName() + " is not open").initCause(e);
          }
          var registryMap = Arrays.stream(nestHost.getNestMembers())
              .filter(Class::isRecord)
              .filter(Component.class::isAssignableFrom)
              .collect(toMap(
                  Class::getSimpleName,
                  clazz -> createRecordFactory(nestHostLookup, clazz)));
          return (name, attributes) -> {
            var componentFactory = registryMap.get(name);
            if (componentFactory == null) {
              throw new IllegalStateException("Unknown component " + name);
            }
            return componentFactory.apply(attributes);
          };
        }
      };

  private static Function<Map<String, Object>, Component> createRecordFactory(Lookup lookup, Class<?> recordClass) {
    var recordComponents = recordClass.getRecordComponents();
    var parameterNames = Arrays.stream(recordComponents)
        .map(RecordComponent::getName)
        .toArray(String[]::new);
    var parameterTypes = Arrays.stream(recordComponents)
        .map(RecordComponent::getType)
        .toArray(Class<?>[]::new);
    MethodHandle constructor;
    try {
      constructor = lookup.findConstructor(recordClass, methodType(void.class, parameterTypes))
          .asSpreader(Object[].class, parameterTypes.length)
          .asType(methodType(Component.class, Object[].class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new IllegalStateException("Cannot find canonical constructor", e);
    }
    return attributes -> {
      var array = new Object[parameterNames.length];
      for(var i = 0; i < array.length; i++) {
        array[i] = attributes.get(parameterNames[i]);
      }
      try {
        return (Component) constructor.invokeExact(array);
      } catch (RuntimeException | Error e) {
        throw e;
      } catch (Throwable e) {
        throw new UndeclaredThrowableException(e);
      }
    };
  }

  private static boolean startsWithAnUpperCase(String name) {
    return !name.isEmpty() && Character.isUpperCase(name.charAt(0));
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

  private static void emitCharacters(Characters characters, List<Object> values, XMLEventFactory eventFactory, Consumer<XMLEvent> consumer) {
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
          renderer.emitEvents(consumer);
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

  private static void emitStartElement(StartElement startElement, List<Object> values, XMLEventFactory eventFactory, Class<?> nestHost, Consumer<XMLEvent> consumer) {
    var name = startElement.getName().getLocalPart();
    var attributeIterator = new AttributeRewriterIterator(startElement.getAttributes(), values);
    if (startsWithAnUpperCase(name)) {
      var component = FACTORY_CACHE.get(nestHost).apply(name, asAttributeMap(attributeIterator));
      component.render().emitEvents(consumer);
      return;
    }
    eventFactory.setLocation(startElement.getLocation());
    var newEvent = eventFactory.createStartElement(startElement.getName(), attributeIterator, startElement.getNamespaces());
    consumer.accept(newEvent);
  }

  @Override
  public Renderer process(StringTemplate stringTemplate) throws RuntimeException {
    Objects.requireNonNull(stringTemplate);
    var callingClass = STACK_WALKER.walk(stream -> stream
        .map(StackWalker.StackFrame::getDeclaringClass)
        .filter(clazz -> !clazz.getPackageName().equals(PACKAGE_NAME))
        .findFirst().orElseThrow());
    var nestHost = callingClass.getNestHost();
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

    return new Renderer() {
      @Override
      public void emitEvents(Consumer<XMLEvent> consumer) {
        var eventFactory = XMLEventFactory.newDefaultFactory();
        while (reader.hasNext()) {
          XMLEvent event;
          try {
            event = reader.nextEvent();
          } catch (XMLStreamException e) {
            throw new NoSuchElementException("error while parsing\n" + text, e);
          }
          switch (event) {
            case StartDocument _, EndDocument _ -> {}
            case Characters characters -> emitCharacters(characters, values, eventFactory, consumer);
            case StartElement startElement -> emitStartElement(startElement, values, eventFactory, nestHost, consumer);
            case EndElement endElement when startsWithAnUpperCase(endElement.getName().getLocalPart()) -> {}
            default -> consumer.accept(event);
          }
        }
      }

      @Override
      public String toString() {
        return asString();
      }
    };
  }
}
