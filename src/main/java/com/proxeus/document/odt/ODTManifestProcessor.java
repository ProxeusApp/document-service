package com.proxeus.document.odt;

import com.proxeus.document.AssetFile;
import com.proxeus.xml.processor.XMLEventProcessor;
import org.apache.log4j.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.util.*;

public class ODTManifestProcessor implements XMLEventProcessor {
    private Logger log = Logger.getLogger(this.getClass());

    static private String MANIFEST_NAMESPACE = "urn:oasis:names:tc:opendocument:xmlns:manifest:1.0";
    static private QName MANIFEST = new QName(MANIFEST_NAMESPACE, "manifext");
    static private QName FILE_ENTRY = new QName(MANIFEST_NAMESPACE, "file-entry");
    static private QName FULL_PATH = new QName(MANIFEST_NAMESPACE, "full-path");
    static private QName MEDIA_TYPE = new QName(MANIFEST_NAMESPACE, "media-type");
    static private XMLEventFactory eventFactory = XMLEventFactory.newInstance();


    private Queue<AssetFile> assetFiles;

    ODTManifestProcessor(Queue<AssetFile> assetFiles) {
        this.assetFiles = assetFiles;
    }

    @Override
    public void process(XMLEventReader reader, XMLEventWriter writer) throws XMLStreamException, IllegalStateException {
        try {
            Set<File> done = new HashSet<>();
            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();

                switch (event.getEventType()) {
                    case XMLEvent.START_ELEMENT:
                        StartElement s = event.asStartElement();
                        process(s, writer);
                        break;
                    case XMLEvent.END_ELEMENT:
                        EndElement e = event.asEndElement();
                        process(e, writer);
                        break;
                    default:
                        writer.add(event);
                }
            }
        } catch (Exception e) {
            //not important as libre will handle it with the repair feature
            log.info("couldn't modify the manifest");
        }
    }

    private void process(StartElement s, XMLEventWriter writer) throws XMLStreamException {
        if (!s.getName().equals(FILE_ENTRY)) {
            writer.add(s);
            return;
        }

        Attribute a = s.getAttributeByName(FULL_PATH);
        if (a == null) {
            writer.add(s);
            return;
        }

        String fullPath = a.getValue();

        boolean match = false;
        for (AssetFile file : assetFiles) {
            if (fullPath.contains(file.orgZipPath)) {
                match = true;
                break;
            }
        }

        if (!match) {
            writer.add(s);
        }
    }

    private void process(EndElement e, XMLEventWriter writer) throws XMLStreamException {
        if (!e.getName().equals(MANIFEST)) {
            writer.add(e);
            return;
        }

        for (AssetFile file : assetFiles) {
            List<Attribute> attributes = Arrays.asList(
                    eventFactory.createAttribute(FULL_PATH, "/" + file.newZipPath.substring(1)),
                    eventFactory.createAttribute(MEDIA_TYPE, "image/png")
            );

            writer.add(eventFactory.createStartElement(FILE_ENTRY, attributes.iterator(), null));
            writer.add(eventFactory.createEndElement(FILE_ENTRY, null));
        }

        writer.add(e);
    }
}
