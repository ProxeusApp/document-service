package com.proxeus.document.odt;

import com.proxeus.compiler.jtwig.MyJTwigCompiler;
import com.proxeus.document.DocumentCompilerIF;
import com.proxeus.document.FileResult;
import com.proxeus.document.FontInstaller;
import com.proxeus.document.Template;
import com.proxeus.error.CompilationException;
import com.proxeus.error.InternalException;
import com.proxeus.error.UnavailableException;
import com.proxeus.office.libre.LibreOfficeAssistant;
import com.proxeus.xml.Config;
import com.proxeus.xml.VarParser;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.proxeus.document.odt.ODTContext.CONTENT_XML;

/**
 * ODTCompiler implements the actual odt specifics to compile it and convert it to the requested format.
 * It makes it possible to parse the vars only and it can print errors readable in the ODT.
 */
public class ODTCompiler implements DocumentCompilerIF {
    //only used for error
    public final static Charset UTF_8 = StandardCharsets.UTF_8;
    private File cacheDir;
    private final FontInstaller fontInstaller;
    private ODTReadableErrorHandler odtReadableRrrorHandler = new ODTReadableErrorHandler();
    private Config conf;
    private LibreOfficeAssistant libreOfficeAssistant;
    private MyJTwigCompiler compiler;

    public ODTCompiler(String cacheFolder, MyJTwigCompiler compiler, LibreOfficeAssistant libreOfficeAssistant) throws Exception {
        this.libreOfficeAssistant = libreOfficeAssistant;
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
         * Add tag names for removal around code.
         * They will be removed if they wrap single code blocks only.
         */
        conf.AddTagNamesForRemovalAroundCode("text:span", "text:p");
        /**
         * Make code placement meaningful in the document.
         */
        conf.FixCodeByFindingTheNextCommonParent = true;
        /**
         * Try to find more suitable nodes to wrap if possible.
         */
        conf.AddTryToWrapXMLTagWithCode("for", "table:table-row");

        this.compiler = compiler;
    }

    public FileResult Compile(Template template) throws Exception {
        return compile(template, null);
    }

    public Set<String> Vars(Template template, String varPrefix) throws Exception {
        VarParser varParser = new VarParser(varPrefix);
        compile(template, varParser);
        return varParser.Vars();
    }

    private FileResult compile(Template template, VarParser varParser) throws Exception {
        ODTContext cfc = new ODTContext(template, varParser);
        try {
            cfc.extractAndCompile(conf, compiler);
        } catch (CompilationException e) {
            cfc.waitForImageTasksToFinish();
            if(template.embedError){
                return renderOdtError(cfc.template, e.getMessage(), cfc.template.tmpDir);
            }else{
                throw e;
            }
        } catch (Exception e) {
            cfc.waitForImageTasksToFinish();
            throw e;
        }
        if (!cfc.readVarsOnly()) {
            try{
                cfc.finish();
            }catch (Exception e){
                throw new InternalException("Couldn't finish up, error during pack to zip.", e);
            }
            try {
                FileResult result = new FileResult(template);
                result.target = new File(cfc.template.tmpDir, "final");
                result.template = cfc.template;
                boolean newFontsInstalled = cfc.extractedFonts && fontInstaller.installDir(cfc.getFontsDir());
                result.contentType = libreOfficeAssistant.Convert(cfc.template.src, result.target, cfc.template.format, newFontsInstalled);
                return result;
            } catch (Exception e) {
                cfc.waitForImageTasksToFinish();
                throw new UnavailableException("LibreOffice error during convert to " + cfc.template.format + " please try again.");
            }
        }
        return null;
    }

    /**
     * Printing error inside the document.
     * TODO refactor string manipulation
     */
    private FileResult renderOdtError(Template template, String error, File userTmpDir) throws Exception {
        String contentXml = "";
        try (ZipFile zipFile = new ZipFile(template.src)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().equalsIgnoreCase(CONTENT_XML)) {
                    contentXml = IOUtils.toString(zipFile.getInputStream(entry), UTF_8);
                    break;
                }
            }
        }
        contentXml = odtReadableRrrorHandler.setErrorMessage(contentXml, error);
        File tmp = new File(userTmpDir, "odterror");
        try (FileOutputStream fos = new FileOutputStream(tmp)) {
            fos.write(contentXml.getBytes(UTF_8));
            fos.flush();
        }
        try (FileSystem fs = FileSystems.newFileSystem(template.src.toPath(), null)) {
            Path fileInsideZipPath = fs.getPath("/" + CONTENT_XML);
            Files.copy(tmp.toPath(), fileInsideZipPath, StandardCopyOption.REPLACE_EXISTING);
        }
        if (!tmp.delete()) {
            System.out.println("renderOdtError tmp.delete() failed for " + tmp.getAbsolutePath());
        }
        FileResult result = new FileResult(template);
        result.target = new File(template.tmpDir, "error");
        result.contentType = libreOfficeAssistant.Convert(template.src, result.target, "pdf", false);
        return result;
    }
}
