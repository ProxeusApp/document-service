package com.proxeus.document.odt.img;

import com.proxeus.document.AssetFile;
import com.proxeus.util.Eval;
import com.proxeus.util.zip.Zip;
import com.proxeus.xml.processor.XMLEventProcessor;

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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ImageAdjustProcessorFactory {

    private final static String DRAW = "urn:oasis:names:tc:opendocument:xmlns:drawing:1.0";
    private final static QName DRAW_FRAME = new QName(DRAW, "frame");
    private final static QName DRAW_IMAGE = new QName(DRAW, "image");
    private final static QName DRAW_NAME = new QName(DRAW, "name");
    private final static String XLINK = "http://www.w3.org/1999/xlink";
    private final static QName XLINK_HREF = new QName(XLINK, "href");
    private final static String SVG = "urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0";
    private final static QName SVG_WIDTH = new QName(SVG, "width");
    private final static QName SVG_HEIGHT = new QName(SVG, "height");
    private final static String LOEXT = "urn:org:documentfoundation:names:experimental:office:xmlns:loext:1.0";
    private final static QName LOEXT_MIMETYPE = new QName(LOEXT, "mime-type");

    private Map<String, Object> data;
    private ExecutorService imgExecutor = Executors.newFixedThreadPool(4);
    private Queue<AssetFile> assetFiles = new ConcurrentLinkedQueue<>();
    private Queue<Exception> exceptions = new ConcurrentLinkedQueue<>();
    private File tmpDir;
    private XMLEventFactory eventFactory = XMLEventFactory.newInstance();

    public ImageAdjustProcessorFactory(File tmpDir, Map<String, Object> data) {
        this.data = data;
        this.tmpDir = tmpDir;
    }

    public XMLEventProcessor newInstance(String entryName) {
        return new ImageAdjustProcessor(entryName);
    }

    public class ImageAdjustProcessor implements XMLEventProcessor {
        private String entryName;

        ImageAdjustProcessor(String entryName) {
            this.entryName = entryName;
        }

        /**
         * styles:
         * <p>
         * <draw:frame draw:style-name="Mfr1" draw:name="Image2" text:anchor-type="paragraph" svg:x="-0.2563in" svg:y="-0.4437in" svg:width="1.428in" svg:height="0.7575in" draw:z-index="7">
         * <draw:image xlink:href="../native_americans_maps.jpg" xlink:type="simple" xlink:show="embed" xlink:actuate="onLoad" draw:filter-name="&lt;All formats&gt;" loext:mime-type="image/jpeg"/>
         * </draw:frame>
         * <p>
         * <p>
         * <draw:frame draw:style-name="fr1" draw:name="{{input.ImageFile2}}" text:anchor-type="paragraph" svg:x="0.0138in"
         * svg:y="0.0409in" svg:width="2.7709in" svg:height="2.328in" draw:z-index="1">
         * <draw:image xlink:href="Pictures/100000000000018F00000113DD14385317835C26.png" xlink:type="simple" xlink:show="embed"
         * xlink:actuate="onLoad" loext:mime-type="image/png"/>
         * </draw:frame>
         */
        @Override
        public void process(XMLEventReader reader, XMLEventWriter writer) throws XMLStreamException, IllegalStateException {
            String xmlDirPath = Zip.dirPath(entryName);
            Queue<XMLEvent> frameEvents = new LinkedList<>();

            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();
                switch (event.getEventType()) {
                    case XMLEvent.START_ELEMENT:
                        StartElement start = event.asStartElement();
                        if (start.getName().equals(DRAW_FRAME)) {
                            frameEvents.add(start);
                            continue;
                        }
                        if (start.getName().equals(DRAW_IMAGE)) {
                            frameEvents.add(start);
                            continue;
                        }
                        writer.add(start);
                        continue;
                    case XMLEvent.END_ELEMENT:
                        EndElement end = event.asEndElement();
                        if (end.getName().equals(DRAW_FRAME)) {
                            frameEvents.add(end);
                            for (XMLEvent e : assetFileReplacement(frameEvents, xmlDirPath)) {
                                writer.add(e);
                            }
                            frameEvents.clear();
                            continue;
                        }
                        if (end.getName().equals(DRAW_IMAGE)) {
                            frameEvents.add(end);
                            continue;
                        }
                        writer.add(end);
                        continue;
                    default:
                        writer.add(event);
                }
            }
        }

        /**
         * Do the necessary changes on the ODT's XML and execute the image adjuster thread.
         * If we are not just looking for vars and if a valid var was specified in the ODT for this very img element.
         *
         * @param frame      img element
         * @param image      child element of img element
         * @param xmlDirPath inside the zip
         */
        Queue<XMLEvent> assetFileReplacement(Queue<XMLEvent> frameEvents, String xmlDirPath) {
            StartElement frame = frameEvents.peek().asStartElement();

            String varWithOptions = attr(frame, DRAW_NAME);
            if (!(varWithOptions.startsWith("{{") && varWithOptions.endsWith("}}"))) {
                //continue as there is no valid var expression and therefore nothing for us to do on this image tag
                return frameEvents;
            }
            // Consume the frame start element
            frameEvents.poll();

            Queue<XMLEvent> result = new LinkedList<>();

            result.add(update(frame, Arrays.asList(
                    eventFactory.createAttribute(DRAW_NAME, "img" + System.nanoTime()))
            ));

            //take away the expression >{{< * >}}<
            varWithOptions = varWithOptions.substring(2, varWithOptions.length() - 2).trim();
            StartElement image = frameEvents.peek().asStartElement();
            ImageSettings imgStngs = new ImageSettings(
                    xmlDirPath,
                    attr(image, XLINK_HREF),
                    varWithOptions,
                    attr(frame, SVG_WIDTH),
                    attr(frame, SVG_HEIGHT),
                    tmpDir,
                    assetFiles
            );

            if (!imgStngs.readyToBeExecuted()) {
                result.addAll(frameEvents);
                return result;
            }

            try {
                imgStngs.touchFile();
            } catch (Exception e) {
                exceptions.offer(e);
                result.addAll(frameEvents);
                return result;
            }

            //resolve the variable synchronously to the image settings so it can be executed asynchronously
            imgStngs.localRemoteOrEmbeddedFileObject = Eval.me(imgStngs.varOnly, data);

            if (imgStngs.localRemoteOrEmbeddedFileObject == null) {
                result.addAll(frameEvents);
                return result;
            }

            // consume the image start event
            frameEvents.poll();
            //the adjusted image will be always a png
            image = update(image,Arrays.asList(
                    //the adjusted image will be always a png
                    eventFactory.createAttribute(LOEXT_MIMETYPE, "image/png"),
                    //make sure it is embedded by forcing the path Pictures/imageSettingsID
                    //this path must be relative
                    //if there is an embedded object like Object 1/Pictures.., the path in the content.xml is still Pictures/.. but not in the root manifest
                    eventFactory.createAttribute(XLINK_HREF, "Pictures/" + imgStngs.ID())
            ));

            result.add(image);
            result.addAll(frameEvents);

            imgExecutor.submit(new ImageAdjusterRunnable(imgStngs, exceptions));

            return result;
        }
    }

    private String attr(StartElement start, QName name) {
        Attribute attribute = start.getAttributeByName(name);
        if (attribute == null) {
            return "";
        }
        return attribute.getValue().trim();
    }

    private StartElement update(StartElement start, List<Attribute> updatedAttributes) {
        Map<QName, Attribute> attributes = new HashMap<>();
        Iterator it = start.getAttributes();
        while (it.hasNext()) {
            Attribute a = (Attribute) it.next();
            attributes.put(a.getName(), a);
        }
        for(Attribute updated : updatedAttributes){
            attributes.put(updated.getName(), updated);
        }

        return eventFactory.createStartElement(start.getName(), attributes.values().iterator(), null);
    }

    public Result finish() throws Exception {
        imgExecutor.shutdown();
        imgExecutor.awaitTermination(15, TimeUnit.SECONDS);
        return new Result(assetFiles, exceptions);
    }


    public class Result {

        private Queue<AssetFile> assetFiles;
        private Queue<Exception> exceptions;

        Result(Queue<AssetFile> assetFiles, Queue<Exception> exceptions) {
            this.assetFiles = assetFiles;
            this.exceptions = exceptions;
        }

        public Queue<AssetFile> getAssetFiles() {
            return assetFiles;
        }

        public Queue<Exception> getExceptions() {
            return exceptions;
        }
    }

}
