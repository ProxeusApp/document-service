package com.proxeus.xml.template;

import com.proxeus.xml.template.parser.TagType;
import com.proxeus.xml.template.parser.ParserState;

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
    private ParserState state;
    private TagType tagType;
    private int blockId;

    public ExtractorXMLEvent(XMLEvent event, ParserState state, TagType tagType, int blockId) {
        this.event = event;
        this.state = state;
        this.tagType = tagType;
        this.blockId = blockId;
    }

    public XMLEvent getEvent() {
        return event;
    }

    public ParserState getState() {
        return state;
    }

    public int getBlockId() {
        return blockId;
    }

    public TagType getTagType() {
        return tagType;
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
