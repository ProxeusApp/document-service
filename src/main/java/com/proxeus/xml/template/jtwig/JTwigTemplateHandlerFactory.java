package com.proxeus.xml.template.jtwig;

import com.proxeus.xml.processor.XMLEventProcessor;
import com.proxeus.xml.processor.XMLEventProcessorChain;
import com.proxeus.xml.template.DefaultTemplateHandler;
import com.proxeus.xml.template.TemplateExtractor;
import com.proxeus.xml.template.TemplateHandler;
import com.proxeus.xml.template.TemplateHandlerFactory;

public class JTwigTemplateHandlerFactory implements TemplateHandlerFactory {

    @Override
    public TemplateHandler newInstance(XMLEventProcessor... processors) {
        XMLEventProcessorChain p = new XMLEventProcessorChain(new TemplateExtractor(new JTwigParser()));
        p.addProcessor(processors);
        return new DefaultTemplateHandler(p, new JTwigRenderer());
    }

}
