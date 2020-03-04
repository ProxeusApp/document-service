package com.proxeus.document.odt;

import com.proxeus.compiler.jtwig.MyJTwigCompiler;
import com.proxeus.xml.Compiled;
import com.proxeus.xml.Node;
import com.proxeus.xml.template.TemplateHandler;

import java.io.*;
import java.util.Map;
import java.util.Queue;

/**
 * ODTCompileRunnable runs the actual compilation async.
 */
public class ODTCompileRunnable implements Runnable {
    private File xmlFile;
    private TemplateHandler xml;
    private Map<String, Object> data;
    private MyJTwigCompiler compiler;
    private Queue<Exception> exceptions;

    public ODTCompileRunnable(MyJTwigCompiler compiler, File xmlFile, TemplateHandler xml, Map<String, Object> data, Queue<Exception> exceptions) {
        this.compiler = compiler;
        //copy data as it will be executed async
        this.data = data;
        //this is not going to be used anymore by the main thread
        this.xml = xml;
        //this is going to be used when the task is finished
        this.xmlFile = xmlFile;
        this.exceptions = exceptions;
    }

    public void run() {
        try (FileOutputStream fos = new FileOutputStream(xmlFile)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            xml.toOutputStream(out);
            System.out.printf("DEBUG XML OUTPUT %s\n", xmlFile);
            InputStream in = new ByteArrayInputStream(out.toByteArray());
            compiler.Compile(in, data, fos, xml.getCharset());

            fos.flush();
            xml.free();
            xmlFile = null;
            xml = null;
            data = null;
            exceptions = null;
        } catch (Exception e) {
            exceptions.offer(e);
        }
    }
}