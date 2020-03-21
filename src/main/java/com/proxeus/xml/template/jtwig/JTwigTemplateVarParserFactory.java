package com.proxeus.xml.template.jtwig;

import com.proxeus.xml.template.TemplateVarParser;
import com.proxeus.xml.template.TemplateVarParserFactory;

public class JTwigTemplateVarParserFactory implements TemplateVarParserFactory {
    @Override
    public TemplateVarParser newInstance() {
        return new JTwigVarParser("");
    }
}
