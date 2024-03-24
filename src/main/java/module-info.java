/**
 * An API that allows to define {@link com.github.forax.htmlcomponent.Component} that are able
 * to render themselves as XML elements.
 */
module com.github.forax.htmlcomponent {
  requires transitive java.xml;

  requires static jdk.httpserver;  // for test only WTF !

  exports com.github.forax.htmlcomponent;
}