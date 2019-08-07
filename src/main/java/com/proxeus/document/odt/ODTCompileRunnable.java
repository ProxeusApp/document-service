package com.proxeus.document.odt;

import com.proxeus.compiler.jtwig.MyJTwigCompiler;
import com.proxeus.xml.Compiled;
import com.proxeus.xml.Node;
import com.proxeus.xml.XmlTemplateHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Queue;

/**
 * ODTCompileRunnable runs the actual compilation async.
 */
public class ODTCompileRunnable implements Runnable {
    private File xmlFile;
    private XmlTemplateHandler xml;
    private Map<String, Object> data;
    private MyJTwigCompiler compiler;
    private Queue<Exception> exceptions;

    public ODTCompileRunnable(MyJTwigCompiler compiler, File xmlFile, XmlTemplateHandler xml, Map<String, Object> data, Queue<Exception> exceptions){
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
        try(FileOutputStream fos = new FileOutputStream(xmlFile)){
            if(xml.containsCode()){
                xml.fixCodeStructures();
                Node rootNodeContainingCode = xml.getRootNodeContainingCode();
                if (rootNodeContainingCode != null) {
                    //need to compile
                    InputStream forCompilation = rootNodeContainingCode.toInputStream(xml.getCharset());
                    Compiled compiled = new Compiled();
                    compiler.Compile(forCompilation, data, compiled.getOutputStream(), xml.getCharset());
                    rootNodeContainingCode.replaceWith(compiled);
                }
            }
            xml.toOutputStream(fos);
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