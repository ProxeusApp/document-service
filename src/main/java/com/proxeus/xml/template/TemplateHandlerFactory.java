package com.proxeus.xml.template;

import com.proxeus.xml.processor.XMLEventProcessor;

public interface TemplateHandlerFactory {
    TemplateHandler newInstance(XMLEventProcessor ...processors);
}
