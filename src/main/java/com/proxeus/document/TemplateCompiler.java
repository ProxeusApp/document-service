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

    public FileResult compile(InputStream zipStream, String format, boolean embedError) throws Exception {
        Template template = provideTemplateFromZIP(zipStream, format);
        template.setEmbedError(embedError);
        return getCompiler(template).Compile(template);
    }

    public Set<String> vars(InputStream odtStream, String varPrefix) throws Exception {
        Template template = provideTemplateFromODT(odtStream);
        return getCompiler(template).Vars(template, varPrefix);
    }

    private DocumentCompiler getCompiler(Template template) {
        switch (template.getType()) {
            case ODT:
            default:
                return odtCompiler;
        }
    }

    private Template provideTemplateFromZIP(InputStream zipStream, String format) throws Exception {
        try {
            if (format == null) {
                format = "pdf";
            }
            Template template = extractZIP(zipStream);
            template.setFormat(format);
            return template;
        } catch (Exception e) {
            throw new BadRequestException("Please read the specification for creating the request with the zip package. zip[tmpl.odt,data.json,assets1,asset2...]");
        }
    }

    private Template provideTemplateFromODT(InputStream zipStream) throws Exception {
        try {
            Template template = new Template();
            template.setSrc(new File(template.getTmpDir(), "tmpl"));
            template.setType(TemplateType.ODT);
            FileUtils.copyToFile(zipStream, template.getSrc());
            return template;
        } catch (Exception e) {
            throw new BadRequestException("Please read the specification for the vars request.");
        }
    }

    /**
     * Extracting the ZIP package.
     * Structure:
     * -zip
     * ---- tmpl.odt | tmpl.docx //only one template supported
     * ---- data.json //the json data the template is going to be resolved with
     * ---- asset1 // assets that should be referenced in the json data
     * ---- asset2
     * ---- asset3
     *
     * @param zipStream ZIP package
     * @return a Template that should be ready to be compiled
     */
    @SuppressWarnings("unchecked")
    private Template extractZIP(InputStream zipStream) throws Exception {
        Template template = new Template();
        Zip.extract(zipStream, new EntryFilter() {
            public void next(ZipEntry zipEntry, InputStream zipInputStream) throws Exception {
                if (zipEntry.getName().toLowerCase().endsWith(".odt")) {
                    //found an odt template inside the zip
                    template.setType(ODT);
                    template.setSrc(Zip.zipEntryToFile(zipEntry, zipInputStream, template.getTmpDir(), "tmpl.odt"));
                    if (!template.getSrc().exists() || template.getSrc().isDirectory()) {
                        throw new BadRequestException("couldn't process template odt");
                    }
                } else if (zipEntry.getName().toLowerCase().endsWith(".docx")) {
                    //found a docx template inside the zip
                    template.setType(DOCX);
                    template.setSrc(Zip.zipEntryToFile(zipEntry, zipInputStream, template.getTmpDir(), "tmpl.docx"));
                    if (!template.getSrc().exists() || template.getSrc().isDirectory()) {
                        throw new BadRequestException("couldn't process template docx");
                    }
                } else if (zipEntry.getName().toLowerCase().endsWith(".json")) {
                    //the json data the template is going to be resolved with
                    int jsonSize = (int) zipEntry.getSize();
                    byte[] jsonBuffer;
                    if (jsonSize > 0) {
                        jsonBuffer = new byte[jsonSize];
                        IOUtils.read(zipInputStream, jsonBuffer);
                    } else {
                        jsonBuffer = IOUtils.toByteArray(zipInputStream);
                    }
                    template.setData(Json.fromJson(jsonBuffer, Map.class));
                } else {
                    //other assets that should be referenced in the json data
                    Zip.zipEntryToFile(zipEntry, zipInputStream, template.getTmpDir(), zipEntry.getName());
                }
            }
        });
        if (template.getSrc() == null) {
            throw new BadRequestException("template not found inside the ZIP");
        }
        if (template.getData() == null) {
            template.setData(new HashMap());
        }
        return template;
    }

}
