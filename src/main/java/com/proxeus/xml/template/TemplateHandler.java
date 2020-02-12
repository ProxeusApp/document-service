package com.proxeus.xml.template;

import com.proxeus.xml.Node;
import com.proxeus.xml.template.jtwig.JTwigVarParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Map;


public interface TemplateHandler {
    void process(InputStream input) throws Exception;

    void render(OutputStream output, Map<String,Object> data) throws Exception;


    boolean containsCode();

    void fixCodeStructures() throws Exception;

    Node getRootNodeContainingCode();

    void findVars(JTwigVarParser varParser) throws Exception;

    Charset getCharset();

    void toOutputStream(OutputStream outputStream) throws IOException;

    void free();

}
