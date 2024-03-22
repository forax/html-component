module com.github.forax.htmlcomponent {
  requires transitive java.xml;

  requires static spring.context;
  requires static java.desktop;

  exports com.github.forax.htmlcomponent;
  exports com.github.forax.htmlcomponent.connector;
}