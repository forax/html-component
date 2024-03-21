package com.github.forax.htmlcomponent;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import java.io.Writer;

record ValueAttribute(Attribute attribute, Object value) implements Attribute {
  @Override
  public QName getName() {
    return attribute.getName();
  }

  @Override
  public String getValue() {
    return String.valueOf(value);
  }

  @Override
  public String getDTDType() {
    return attribute.getDTDType();
  }

  @Override
  public boolean isSpecified() {
    return attribute.isSpecified();
  }

  @Override
  public int getEventType() {
    return attribute.getEventType();
  }

  @Override
  public Location getLocation() {
    return attribute.getLocation();
  }

  @Override
  public boolean isStartElement() {
    return attribute.isStartElement();
  }

  @Override
  public boolean isAttribute() {
    return attribute.isAttribute();
  }

  @Override
  public boolean isNamespace() {
    return attribute.isNamespace();
  }

  @Override
  public boolean isEndElement() {
    return attribute.isEndElement();
  }

  @Override
  public boolean isEntityReference() {
    return attribute.isEntityReference();
  }

  @Override
  public boolean isProcessingInstruction() {
    return attribute.isProcessingInstruction();
  }

  @Override
  public boolean isCharacters() {
    return attribute.isCharacters();
  }

  @Override
  public boolean isStartDocument() {
    return attribute.isStartDocument();
  }

  @Override
  public boolean isEndDocument() {
    return attribute.isEndDocument();
  }

  @Override
  public StartElement asStartElement() {
    return attribute.asStartElement();
  }

  @Override
  public EndElement asEndElement() {
    return attribute.asEndElement();
  }

  @Override
  public Characters asCharacters() {
    return attribute.asCharacters();
  }

  @Override
  public QName getSchemaType() {
    return attribute.getSchemaType();
  }

  @Override
  public void writeAsEncodedUnicode(Writer writer) throws XMLStreamException {
    attribute.writeAsEncodedUnicode(writer);
  }
}