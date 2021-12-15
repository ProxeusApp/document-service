package com.proxeus.xml.template.jtwig;

import com.proxeus.compiler.jtwig.MyJTwigCompiler;
import com.proxeus.xml.template.TemplateRenderer;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Map;

public class JTwigRenderer implements TemplateRenderer {
    private Logger log = LogManager.getLogger(this.getClass());

    private MyJTwigCompiler compiler = new MyJTwigCompiler();

    @Override
    public void render(InputStream input, OutputStream output, Map<String, Object> data, Charset charset) throws Exception {
        compiler.Compile(input, data, output, charset);
    }
}
