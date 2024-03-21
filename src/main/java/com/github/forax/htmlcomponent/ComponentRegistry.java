package com.github.forax.htmlcomponent;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static java.lang.invoke.MethodType.methodType;

public final class ComponentRegistry implements Component.Resolver {
  private final ConcurrentHashMap<String, Function<Map<String, Object>, Component>> registry = new ConcurrentHashMap<>();

  public ComponentRegistry() {}

  public Component getComponent(String name, Map<String, Object> attributes) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(attributes);
    var componentFactory = registry.get(name);
    if (componentFactory == null) {
      throw new IllegalStateException("unknown component " + name);
    }
    return componentFactory.apply(attributes);
  }

  public void registerFactory(String name, Function<Map<String, Object>, Component> componentFactory) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(componentFactory);
    if (registry.putIfAbsent(name, componentFactory) != null) {
      throw new IllegalStateException("a component with the name " + name + " is already registered");
    }
  }

  public void register(Lookup lookup, Class<? extends Record> recordClass) {
    Objects.requireNonNull(lookup);
    Objects.requireNonNull(recordClass);
    var recordComponents = recordClass.getRecordComponents();
    if (recordComponents == null) {
      throw new IllegalArgumentException("invalid record class " + recordClass.getName());
    }
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
      throw new IllegalStateException(e);
    }
    registerFactory(recordClass.getSimpleName(), attributes -> {
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
