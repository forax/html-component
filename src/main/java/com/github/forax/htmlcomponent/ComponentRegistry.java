package com.github.forax.htmlcomponent;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static java.lang.invoke.MethodType.methodType;
import static java.util.stream.Collectors.toMap;

/**
 * Implementation of the interface {@link com.github.forax.htmlcomponent.Component.Resolver}
 * that stores record ({@link Class}).
 * <p>
 * When {@link #getComponent(String, Map)}, the class corresponding to the name is instantiated
 * by calling the canonical constructor of the record using the name of the record components.
 * <p>
 * This class is thread safe.
 */
public final class ComponentRegistry implements Component.Resolver {

  private record RecordInfo(String name, Function<Map<String, Object>, Component> componentFactory) {}

  private static  RecordInfo createRecordInfo(Lookup lookup, Class<?> recordClass) {
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
    return new RecordInfo(recordClass.getSimpleName(), attributes -> {
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

  private final Map<String, Function<Map<String, Object>, Component>> reguistryMap;

  private ComponentRegistry(Map<String, Function<Map<String, Object>, Component>> reguistryMap) {
    this.reguistryMap = reguistryMap;
  }

  @Override
  public Component getComponent(String name, Map<String, Object> attributes) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(attributes);
    var componentFactory = reguistryMap.get(name);
    if (componentFactory == null) {
      throw new IllegalStateException("unknown component factory for " + name);
    }
    return componentFactory.apply(attributes);
  }

  /**
   * Creates a registry configured with the record classes.
   *
   * @param lookup a lookup able to call the constructor of the record classes
   * @param recordClasses the record classes.
   * @return a new registry configured with the record classes
   */
  @SafeVarargs
  public static ComponentRegistry getRegistry(Lookup lookup, Class<? extends Record>... recordClasses) {
    Objects.requireNonNull(lookup);
    Objects.requireNonNull(recordClasses);
    var registryMap = Arrays.stream(recordClasses)
        .map(recordClass -> createRecordInfo(lookup, recordClass))
        .collect(toMap(RecordInfo::name, RecordInfo::componentFactory));
    return new ComponentRegistry(registryMap);
  }
}
