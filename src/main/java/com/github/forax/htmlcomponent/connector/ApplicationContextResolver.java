package com.github.forax.htmlcomponent.connector;

import com.github.forax.htmlcomponent.Component;
import org.springframework.context.ApplicationContext;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Map;

/**
 * Component resolver using Spring application context
 */
public class ApplicationContextResolver implements Component.Resolver {
  private final ApplicationContext context;

  public ApplicationContextResolver(ApplicationContext context) {
     this.context = context;
  }

  @Override
  public Component getComponent(String name, Map<String, Object> attributes) {
    var beanName = Introspector.decapitalize(name);

    var beanType = context.getType(beanName);
    if (beanType == null) {
      throw new IllegalStateException("no bean type definition for bean name " + beanName);
    }
    if (!Component.class.isAssignableFrom(beanType)) {
      throw new IllegalStateException("bean " + beanName + " does not implement the interface Component");
    }
    BeanInfo beanInfo;
    try {
      beanInfo = Introspector.getBeanInfo(beanType);
    } catch (IntrospectionException e) {
      throw new IllegalStateException(e);
    }

    var bean = (Component) context.getBean(beanName);
    for(var property: beanInfo.getPropertyDescriptors()) {
      var value = attributes.get(property.getName());
      if (value != null) {
        var writeMethod = property.getWriteMethod();
        if (writeMethod == null) {
          throw new IllegalStateException("no setter for property " + property.getName() + " of bean " + name +  " found");
        }
        try {
          writeMethod.invoke(bean, value);
        } catch (IllegalAccessException e) {
          throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
          switch (e.getCause()) {
            case RuntimeException runtimeException -> throw runtimeException;
            case Error error -> throw error;
            case Throwable throwable -> throw new UndeclaredThrowableException(throwable);
          }
        }
      }
    }
    return bean;
  }
}
