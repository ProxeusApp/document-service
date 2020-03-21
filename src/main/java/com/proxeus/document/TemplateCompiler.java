package com.proxeus.document;

import com.proxeus.document.odt.ODTCompiler;
import com.proxeus.error.BadRequestException;
import com.proxeus.util.Json;
import com.proxeus.util.zip.EntryFilter;
import com.proxeus.util.zip.Zip;
import com.proxeus.xml.template.TemplateHandlerFactory;
import com.proxeus.xml.template.TemplateVarParserFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;

import static com.proxeus.document.TemplateType.DOCX;
import static com.proxeus.document.TemplateType.ODT;


public class TemplateCompiler {
    private Logger log = Logger.getLogger(this.getClass());

    private ODTCompiler odtCompiler;

    public TemplateCompiler(String cacheFolder, TemplateFormatter templateFormatter, TemplateHandlerFactory templateHandlerFactory, TemplateVarParserFactory templateVarParserFactory) throws Exception {
        this.odtCompiler = new ODTCompiler(cacheFolder, templateFormatter, templateHandlerFactory, templateVarParserFactory);
    }

    public FileResult compile(Template template, boolean embedError) throws Exception {
        template.setEmbedError(embedError);
        return getCompiler(template).Compile(template);
    }

    public Set<String> vars(Template template, String varPrefix) throws Exception {
        return getCompiler(template).Vars(template, varPrefix);
    }

    private DocumentCompiler getCompiler(Template template) {
        switch (template.getType()) {
            case ODT:
            default:
                return odtCompiler;
        }
    }
}
