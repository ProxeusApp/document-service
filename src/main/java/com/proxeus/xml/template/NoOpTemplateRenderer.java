package com.proxeus.xml.template;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Map;

public class NoOpTemplateRenderer implements TemplateRenderer {
    @Override
    public void render(InputStream input, OutputStream output, Map<String, Object> data, Charset charset) throws Exception {
        IOUtils.copy(input, output);
    }
}
