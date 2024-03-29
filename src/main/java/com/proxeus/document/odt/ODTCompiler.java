package com.proxeus.document.odt;

import com.proxeus.document.*;
import com.proxeus.document.odt.img.ImageVarProcessor;
import com.proxeus.util.zip.EntryFileFilter;
import com.proxeus.util.zip.Zip;
import com.proxeus.xml.Config;
import com.proxeus.xml.template.*;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.proxeus.document.odt.ODTRenderer.CONTENT_XML;
import static com.proxeus.document.odt.ODTRenderer.STYLE_XML;

/**
 * ODTCompiler implements the actual odt specifics to compile it and convert it to the requested format.
 * It makes it possible to parse the vars only and it can print errors readable in the ODT.
 */
public class ODTCompiler implements DocumentCompiler {
    private Logger log = LogManager.getLogger(this.getClass());

    //only used for error
    public final static Charset UTF_8 = StandardCharsets.UTF_8;
    private File cacheDir;
    private final FontInstaller fontInstaller;
    private Config conf;
    private TemplateFormatter templateFormatter;
    private TemplateHandlerFactory templateHandlerFactory;
    private TemplateVarParserFactory templateVarParserFactory;

    public ODTCompiler(String cacheFolder,
                       TemplateFormatter templateFormatter,
                       TemplateHandlerFactory templateHandlerFactory,
                       TemplateVarParserFactory templateVarParserFactory) throws Exception {
        this.templateFormatter = templateFormatter;
        this.templateHandlerFactory = templateHandlerFactory;
        this.templateVarParserFactory = templateVarParserFactory;

        fontInstaller = new FontInstaller();
        if (cacheFolder == null || cacheFolder.equals("")) {
            cacheFolder = "odtCache";
            cacheDir = new File(System.getProperty("java.io.tmpdir"), cacheFolder);
        } else {
            cacheDir = new File(cacheFolder);
        }
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            throw new Exception("couldn't create cache dir" + cacheDir.getAbsolutePath());
        }
        FileUtils.cleanDirectory(cacheDir);
        conf = new Config();
        /**
         * This is only useful if you provide a broken XML that can not be read anymore.
         */
        conf.Fix_XMLTags = false;

        /**
         * Make code placement meaningful in the document.
         */
        conf.FixCodeByFindingTheNextCommonParent = true;


        // TODO: Need to improve the parser to handle table rows, i.e. extending the block instead of splitting it.
        /**
         * Try to find more suitable nodes to wrap if possible.
         */
        conf.AddTryToWrapXMLTagWithCode("for", "table:table-row");
    }

    public FileResult Compile(Template template) throws Exception {
        return compile(template);
    }

    public Set<String> Vars(Template template, String prefix) throws Exception {
        TemplateVarParser varParser = templateVarParserFactory.newInstance();
        findVars(template, varParser);
        Set<String> result = varParser.getVars().stream().filter(Objects::nonNull).filter(var -> var.startsWith(prefix != null ? prefix : "")).collect(Collectors.toSet());
        return result;
    }

    private void findVars(Template template, TemplateVarParser varParser) throws Exception {
        ImageVarProcessor imageVarProcessor = new ImageVarProcessor(varParser);
        TemplateVarProcessor templateVarProcessor = new TemplateVarProcessor(varParser);
        Zip.extract(template.getSrc(), (entry, zf) -> {
            if (entry.getName().endsWith(CONTENT_XML) || entry.getName().endsWith(STYLE_XML)) {
                TemplateHandler xml = templateHandlerFactory.newInstance(
                        imageVarProcessor,
                        templateVarProcessor
                );
                xml.process(zf.getInputStream(entry));
            }
        });
    }

    private FileResult compile(Template template) throws Exception {
        ODTRenderer cfc = new ODTRenderer(template, templateHandlerFactory, templateFormatter);
        return cfc.compile(conf, fontInstaller);
    }
}
