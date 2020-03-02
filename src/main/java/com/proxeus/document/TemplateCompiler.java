package com.proxeus.document;

import com.proxeus.compiler.jtwig.MyJTwigCompiler;
import com.proxeus.document.docx.DOCXCompiler;
import com.proxeus.document.odt.ODTCompiler;
import com.proxeus.error.BadRequestException;
import com.proxeus.office.libre.LibreOfficeAssistant;
import com.proxeus.office.microsoft.MicrosoftOfficeAssistant;
import com.proxeus.util.Json;
import com.proxeus.util.zip.EntryFilter;
import com.proxeus.util.zip.Zip;

import com.proxeus.xml.template.TemplateHandlerFactory;
import com.proxeus.xml.template.TemplateVarParserFactory;
import com.proxeus.xml.template.jtwig.JTwigTemplateHandlerFactory;
import com.proxeus.xml.template.jtwig.JTwigTemplateVarParserFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;

import static com.proxeus.document.TemplateType.DOCX;
import static com.proxeus.document.TemplateType.ODT;


public class TemplateCompiler {
    private ODTCompiler odtCompiler;
    private DOCXCompiler docxCompiler;
    private MyJTwigCompiler compiler;
    private TemplateHandlerFactory templateHandlerFactory;
    private TemplateVarParserFactory templateVarParserFactory;

    public TemplateCompiler(String cacheFolder, LibreOfficeAssistant libreOfficeAssistant) throws Exception {
        compiler = new MyJTwigCompiler();
        templateHandlerFactory = new JTwigTemplateHandlerFactory();
        templateVarParserFactory = new JTwigTemplateVarParserFactory();
        odtCompiler = new ODTCompiler(cacheFolder, compiler, libreOfficeAssistant, templateHandlerFactory, templateVarParserFactory);
        docxCompiler = new DOCXCompiler(cacheFolder, compiler, new MicrosoftOfficeAssistant(), templateHandlerFactory);
    }

    public FileResult compile(InputStream zipStream, String format, boolean embedError) throws Exception {
        Template template = provideTemplateFromZIP(zipStream, format);
        template.embedError = embedError;
        return getCompiler(template).Compile(template);
    }

    public Set<String> vars(InputStream odtStream, String varPrefix) throws Exception {
        Template template = provideTemplateFromODT(odtStream);
        return getCompiler(template).Vars(template, varPrefix);
    }

    private DocumentCompiler getCompiler(Template template) {
        switch (template.type) {
            case DOCX:
                return docxCompiler;
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
            template.format = format;
            return template;
        } catch (Exception e) {
            throw new BadRequestException("Please read the specification for creating the request with the zip package. zip[tmpl.odt,data.json,assets1,asset2...]");
        }
    }

    private Template provideTemplateFromODT(InputStream zipStream) throws Exception {
        try {
            Template template = new Template();
            template.src = new File(template.tmpDir, "tmpl");
            template.type = TemplateType.ODT;
            FileUtils.copyToFile(zipStream, template.src);
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
                    template.type = ODT;
                    template.src = Zip.zipEntryToFile(zipEntry, zipInputStream, template.tmpDir, "tmpl.odt");
                    if (!template.src.exists() || template.src.isDirectory()) {
                        throw new BadRequestException("couldn't process template odt");
                    }
                } else if (zipEntry.getName().toLowerCase().endsWith(".docx")) {
                    //found a docx template inside the zip
                    template.type = DOCX;
                    template.src = Zip.zipEntryToFile(zipEntry, zipInputStream, template.tmpDir, "tmpl.docx");
                    if (!template.src.exists() || template.src.isDirectory()) {
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
                    template.data = Json.fromJson(jsonBuffer, Map.class);
                } else {
                    //other assets that should be referenced in the json data
                    Zip.zipEntryToFile(zipEntry, zipInputStream, template.tmpDir, zipEntry.getName());
                }
            }
        });
        if (template.src == null) {
            throw new BadRequestException("template not found inside the ZIP");
        }
        if (template.data == null) {
            template.data = new HashMap();
        }
        return template;
    }

}
