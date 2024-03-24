# html-component
A very small library to define unmodifiable HTML/XML components in Java

Html components are
- safe: the template is not string based and checked by an XML parser
- composable: a component can reference another component statically or dynamically
- lightweight: the whole jar is less than 20kb
- defined in Java: no need to learn another template language, and it works with all Java IDEs.

A html component is a record that implements the interface `Component` thus provides a `render` method.

```java
record HelloWorld(String name) implements Component {
  public Renderer render() {
    return $."""
      <div>Hello \{name} !</div>
      """;
  } 
}
```

It uses the mechanism of StringTemplate Processor in preview in Java 21/22 ([JEP 459](https://openjdk.org/jeps/459))
to generate XML events from a template. This makes it impossible to generate invalid fragment of an XML document.

Because it's a plain record, it can be instantiated like any Java classes using `new`
```java
var hello = new HelloWorld("html component");
System.out.println(hello.render());
```

A html component can reference statically another html component if they are both declared in the same class
The name of the component has to start with a letter in uppercase to differentiate it from a regular XML element.
```java
record TwoHello(String name1, String name2) implements Component {
  public Renderer render() {
    return $."""
      <div>
        <HelloWord name="\{name1}"/>
        <HelloWord name="\{name2}"/>
      </div>
      """;
  }
}
```

A html component can reference dynamically any other components, using the method `Renderer.from(stream)`
```java
record HelloList(List<HelloWorld> hellos) implements Component {
  public Renderer render() {
    return $."""
      <div>
        \{ Renderer.from(hellos.stream()) }
      </div>
      """;
  }
}
```

Here are 3 more demos ([Demo.java](test/main/java/Demo.java), [Demo2.java](test/main/java/Demo2.java) and
[DemoList.java](test/main/java/DemoList.java)).
And that's all for the spec part.

## Using html components with htmx

Html components are a good fit for [htmx](https://htmx.org/) because it's an easy way to create XML fragments
that can be downloaded and patched by the JavaScript library htmx.

[HTMXDemo](src/test/java/com/github/forax/test/htmx/HTMXDemo.java) shows how html components can be used
in the context of htmx. The demo using a Java port of the Express.js library
([JExpress.java](src/test/java/com/github/forax/test/htmx/JExpress.java))
but this is not a requirement, it makes just the demo code simpler than using Spring Boot.
