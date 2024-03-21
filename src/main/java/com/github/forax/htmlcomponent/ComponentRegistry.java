package com.github.forax.htmlcomponent;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class ComponentRegistry {
  public static final ComponentRegistry REGISTRY = new ComponentRegistry();

  private final HashMap<String, Function<Map<String, Object>, Component>> registry = new HashMap<>();

  private ComponentRegistry() {}

  public Component getComponent(String name, Map<String, Object> attributes) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(attributes);
    var componentFactory = registry.get(name);
    if (componentFactory == null) {
      throw new IllegalStateException("unknown component " + name);
    }
    return componentFactory.apply(attributes);
  }

  private void registerFactory(String name, Function<Map<String, Object>, Component> componentFactory) {
    if (registry.putIfAbsent(name, componentFactory) != null) {
      throw new IllegalStateException("a component with the name " + name + " is already registered");
    }
  }

  public void register(Lookup lookup, Class<? extends Record> recordClass) {
    Objects.requireNonNull(lookup);
    Objects.requireNonNull(recordClass);
    for(var recordComponent : recordClass.getRecordComponents()) {
      if (!recordComponent.getName().equals("attributes")) {
        throw new IllegalStateException("the record should have a component named attributes");
      }
      if (recordComponent.getType() != Map.class) {
        throw new IllegalStateException("the record attributes should be a Map<String, Object>");
      }
      // TODO more
    }
    MethodHandle constructor;
    try {
      constructor = lookup.findConstructor(recordClass, MethodType.methodType(void.class, Map.class))
          .asType(MethodType.methodType(Component.class, Map.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
    registerFactory(recordClass.getSimpleName(), attributes -> {
      try {
        return (Component) constructor.invokeExact(attributes);
      } catch (RuntimeException | Error e) {
        throw e;
      } catch (Throwable e) {
        throw new UndeclaredThrowableException(e);
      }
    });
  }

  @SafeVarargs
  public final void register(Lookup lookup, Class<? extends Record>... recordClasses) {
    Objects.requireNonNull(lookup);
    Objects.requireNonNull(recordClasses);
    for(var recordClass: recordClasses) {
      register(lookup, recordClass);
    }
  }
}
