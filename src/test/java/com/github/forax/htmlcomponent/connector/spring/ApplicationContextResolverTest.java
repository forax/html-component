package com.github.forax.htmlcomponent.connector.spring;

import com.github.forax.htmlcomponent.connector.ApplicationContextResolver;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ComponentScan
public class ApplicationContextResolverTest {
  @Test
  public void testSpringApplicationContext() {
    var context = new AnnotationConfigApplicationContext(ApplicationContextResolverTest.class);
    var resolver = new ApplicationContextResolver(context);
    var component = resolver.getComponent("ProductBean", Map.of("name", "coffee"));
    assertEquals("<div>coffee</div>", component.render().toString(resolver));
  }
}
