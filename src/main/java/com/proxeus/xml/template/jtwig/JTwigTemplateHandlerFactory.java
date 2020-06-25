package com.proxeus.xml.template.jtwig;

import com.proxeus.compiler.jtwig.MyJTwigCompiler;
import com.proxeus.xml.processor.XMLEventProcessor;
import com.proxeus.xml.processor.XMLEventProcessorChain;
import com.proxeus.xml.template.*;

public class JTwigTemplateHandlerFactory implements TemplateHandlerFactory {

    @Override
    public TemplateHandler newInstance(XMLEventProcessor preRendering, XMLEventProcessor postRendering){
        XMLEventProcessorChain pre = new XMLEventProcessorChain(new TemplateExtractor(new JTwigParser()));
        pre.addProcessor(preRendering);

        XMLEventProcessorChain post = new XMLEventProcessorChain(postRendering, new CleanTemplateExtractorMarkProcessor());
        return new DefaultTemplateHandler(pre, new JTwigRenderer(), post);
    }
}
