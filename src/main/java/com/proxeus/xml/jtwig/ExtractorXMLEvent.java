package com.proxeus.xml.jtwig;

import sun.nio.cs.ext.ISCII91;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.Writer;

public class ExtractorXMLEvent implements XMLEvent {

    private XMLEvent event;
    private ExtractorState state;
    private IslandType islandType;

    public ExtractorXMLEvent(XMLEvent event, ExtractorState state, IslandType islandType) {
        this.event = event;
        this.state = state;
        this.islandType = islandType;
    }

    public XMLEvent getEvent() {
        return event;
    }

    public ExtractorState getState() {
        return state;
    }

    @Override
    public int getEventType() {
        return event.getEventType();
    }

    @Override
    public Location getLocation() {
        return event.getLocation();
    }

    @Override
    public boolean isStartElement() {
        return event.isStartElement();
    }

    @Override
    public boolean isAttribute() {
        return event.isAttribute();
    }

    @Override
    public boolean isNamespace() {
        return event.isNamespace();
    }

    @Override
    public boolean isEndElement() {
        return event.isEndElement();
    }

    @Override
    public boolean isEntityReference() {
        return event.isEntityReference();
    }

    @Override
    public boolean isProcessingInstruction() {
        return event.isProcessingInstruction();
    }

    @Override
    public boolean isCharacters() {
        return event.isCharacters();
    }

    @Override
    public boolean isStartDocument() {
        return event.isStartDocument();
    }

    @Override
    public boolean isEndDocument() {
        return event.isEndDocument();
    }

    @Override
    public StartElement asStartElement() {
        return event.asStartElement();
    }

    @Override
    public EndElement asEndElement() {
        return event.asEndElement();
    }

    @Override
    public Characters asCharacters() {
        return event.asCharacters();
    }

    @Override
    public QName getSchemaType() {
        return event.getSchemaType();
    }

    @Override
    public void writeAsEncodedUnicode(Writer writer) throws XMLStreamException {
        event.writeAsEncodedUnicode(writer);
    }

    public String toString() {
        return event.toString();
    }
}
