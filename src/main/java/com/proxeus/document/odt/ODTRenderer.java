package com.proxeus.document.odt;

import com.proxeus.document.*;
import com.proxeus.document.odt.img.ImageAdjustProcessorFactory;
import com.proxeus.error.CompilationException;
import com.proxeus.error.InternalException;
import com.proxeus.error.UnavailableException;
import com.proxeus.util.zip.EntryFileFilter;
import com.proxeus.util.zip.Zip;
import com.proxeus.xml.Config;
import com.proxeus.xml.template.CleanEmptyElementProcessor;
import com.proxeus.xml.template.TemplateHandler;
import com.proxeus.xml.template.TemplateHandlerFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import javax.xml.namespace.QName;
import java.io.*;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * ODTRenderer takes away the complexity from ODTCompiler and the image handling
 * by keeping the things that are needed from the beginning until the end here together.
 */
public class ODTRenderer {
    private Logger log = Logger.getLogger(this.getClass());

    private static String TEXT = "urn:oasis:names:tc:opendocument:xmlns:text:1.0";
    private static QName TEXT_SPAN = new QName(TEXT, "span");

    private static List<QName> EMPTY_XML_ELEMENT_TO_REMOVE = Arrays.asList(TEXT_SPAN);

    final static String CONTENT_XML = "content.xml";
    final static String STYLE_XML = "styles.xml";
    public Template template;
    private boolean extractedFonts;
    private TemplateHandler manifest = null;
    private File manifestDest = null;
    private ByteArrayOutputStream manifestContent = null;
    private Queue<String> fileToRemoveFromZip = new ConcurrentLinkedQueue<>();


    private ImageAdjustProcessorFactory imageAdjuster;
    private TemplateHandlerFactory templateHandlerFactory;

    private TemplateFormatter templateFormatter;

    ODTRenderer(Template template, TemplateHandlerFactory templateHandlerFactory, TemplateFormatter templateFormatter) {
        this.template = template;
        this.templateHandlerFactory = templateHandlerFactory;
        this.templateFormatter = templateFormatter;
        this.imageAdjuster = new ImageAdjustProcessorFactory(template.getTmpDir(), template.getDataCopy());
    }


    protected FileResult compile(Config conf, FontInstaller fontInstaller) throws Exception {
        try {
            List<File> extractedFiles = extractAndCompile(conf);
            assembleZipFile(extractedFiles);
        } catch (CompilationException e) {
            if (template.isEmbedError()) {
                return renderOdtError(template, e.getMessage(), template.getTmpDir());
            } else {
                throw e;
            }
        }
        return format(fontInstaller);
    }


    private List<File> extractAndCompile(Config conf) throws Exception {
        log.debug(String.format("DEBUG TEMPLATE %s\n", template));

        ExecutorService compileExecutor = Executors.newFixedThreadPool(2);
        List<File> extractedFiles = new ArrayList<>(2);
        Queue<Exception> compileExceptions = new ConcurrentLinkedQueue<>();
        try {
            Zip.extract(template.getSrc(), new EntryFileFilter() {
                public void next(ZipEntry entry, ZipFile zf) throws Exception {
                    if (entry.getName().startsWith("Fonts/")) {
                        extractedFonts = true;
                        File toExtract = new File(template.getTmpDir(), entry.getName());
                        toExtract.getParentFile().mkdirs();
                        FileUtils.copyToFile(zf.getInputStream(entry), toExtract);
                    } else if (entry.getName().equals("META-INF/manifest.xml")) {
                        manifestDest = new File(template.getTmpDir(), entry.getName());
                        manifestDest.getParentFile().mkdirs();
                        manifestContent = new ByteArrayOutputStream();
                        IOUtils.copy(zf.getInputStream(entry), manifestContent);
                    } else if (entry.getName().endsWith(CONTENT_XML) || entry.getName().endsWith(STYLE_XML)) {
                        TemplateHandler xml = templateHandlerFactory.newInstance(
                                new CleanEmptyElementProcessor(EMPTY_XML_ELEMENT_TO_REMOVE),
                                imageAdjuster.newInstance(entry.getName())

                        );
                        xml.process(zf.getInputStream(entry));


                        File toExtract = new File(template.getTmpDir(), entry.getName());
                        toExtract.getParentFile().mkdirs();
                        log.debug(String.format("DEBUG TO EXTRACT %s\n", toExtract));
                        extractedFiles.add(toExtract);

                        compileExecutor.submit(() -> {
                            try {
                                FileOutputStream output = new FileOutputStream(toExtract);
                                xml.render(output, template.getDataCopy());
                                output.flush();
                            } catch (Exception e) {
                                compileExceptions.offer(e);
                            }
                        });
                    }
                }
            });

            compileExecutor.shutdown();
            compileExecutor.awaitTermination(15, TimeUnit.SECONDS);
            if (compileExceptions.size() > 0) {
                //at least one exception was thrown during compilation, throw the first one
                //lets assume the first exception is accurate enough to help solving the issue
                throw compileExceptions.poll();
            }
        } catch (Exception e) {
            waitForImageTasksToFinish();
            throw e;
        }
        return extractedFiles;
    }

    void assembleZipFile(List<File> extractedFiles) throws Exception {
        Set<String> collectedEmbeddedObjects = new HashSet<>();
        log.debug(String.format("DEBUG FINISH FILESYSTEM PATH %s\n", template.getSrc().toPath()));
        try (FileSystem fs = FileSystems.newFileSystem(template.getSrc().toPath(), null)) {
            for (File newPath : extractedFiles) {
                log.debug(String.format("DEBUG FINISH EXTRACTED FILE %s\n", newPath));
                String zipPath = getZipPath(template.getTmpDir(), newPath.getAbsolutePath());
                try {
                    if (zipPath.startsWith(File.separator + "Object")) {
                        collectedEmbeddedObjects.add(zipPath.split(File.separator)[1]);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Files.move(newPath.toPath(), fs.getPath(zipPath), StandardCopyOption.REPLACE_EXISTING);
            }
            for (String embeddedCacheToDelete : collectedEmbeddedObjects) {
                Zip.delete(fs.getPath("/ObjectReplacements/" + embeddedCacheToDelete));
            }
            if (fileToRemoveFromZip != null && fileToRemoveFromZip.size() > 0) {
                for (String toRemPath : fileToRemoveFromZip) {
                    Zip.delete(fs.getPath(toRemPath));
                }
            }

            Queue<AssetFile> assetFiles = waitForImageTasksToFinish();
            processManifest(assetFiles);
            insertManifest(fs);
            includeAssets(assetFiles, fs);
        } catch (Exception e) {
            throw new InternalException("Couldn't finish up, error during pack to zip.", e);
        }
    }

    private FileResult format(FontInstaller fontInstaller) throws UnavailableException {
        try {
            FileResult result = new FileResult(template);
            result.target = new File(template.getTmpDir(), "final");
            result.template = template;
            boolean newFontsInstalled = extractedFonts && fontInstaller.installDir(getFontsDir());
            result.contentType = templateFormatter.Convert(template.getSrc(), result.target, template.getFormat(), newFontsInstalled);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new UnavailableException("LibreOffice error during convert to " + template.getFormat() + ": " + e.getMessage());
        }
    }

    File getFontsDir() {
        return new File(template.getTmpDir(), "Fonts");
    }

    Queue<AssetFile> waitForImageTasksToFinish() throws Exception {
        ImageAdjustProcessorFactory.Result result = imageAdjuster.finish();
        if (result.getExceptions().size() > 0) {
            Exception e = result.getExceptions().poll();
            if (e != null) {
                throw e;
            }
        }
        return result.getAssetFiles();
    }

    private void processManifest(Queue<AssetFile> assetFiles) throws Exception {
        try {
            log.debug(String.format("DEBUG PROCESS MANIFEST %s\n", assetFiles));
            if (manifestContent == null) {
                return;
            }

            TemplateHandler manifest = templateHandlerFactory.newInstance(
                    new ODTManifestProcessor(assetFiles)
            );
            manifest.process(new ByteArrayInputStream(manifestContent.toByteArray()));


            log.debug(String.format("DEBUG PROCESS MANIFEST DEST %s\n", manifestDest));
            FileOutputStream output = new FileOutputStream(manifestDest);

            manifest.render(output, Collections.emptyMap());
        } catch (Exception e) {
            log.debug("DEBUG PROCESS MANIFEST EXCEPTION", e);
            throw e;
        }
    }


    private void insertManifest(FileSystem fs) {
        try {
            Files.move(manifestDest.toPath(), fs.getPath(getZipPath(template.getTmpDir(), manifestDest.getAbsolutePath())), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            log.debug("DEBUG INSERT MANIFEST EXCEPTION", e);
        }
    }

    private void includeAssets(Queue<AssetFile> assetFiles, FileSystem fs) throws IOException {
        for (AssetFile f : assetFiles) {
            if (!f.dst.exists()) {
                //looks like it as been already moved to the zip
                continue;
            }
            Path insideZip = fs.getPath(f.newZipPath);
            try {
                Files.move(f.dst.toPath(), insideZip, StandardCopyOption.REPLACE_EXISTING);
            } catch (NoSuchFileException nsf) {
                Files.createDirectories(insideZip.getParent());
                Files.move(f.dst.toPath(), insideZip, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static String getZipPath(File userTmpDir, String path) {
        String extractedDir = userTmpDir.getName() + File.separator;
        return File.separator + path.substring(path.lastIndexOf(extractedDir) + extractedDir.length());
    }

    /**
     * Printing error inside the document.
     */
    private FileResult renderOdtError(Template template, String error, File userTmpDir) throws Exception {
        String contentXml = "";
        try (ZipFile zipFile = new ZipFile(template.getSrc())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().equalsIgnoreCase(CONTENT_XML)) {
                    contentXml = IOUtils.toString(zipFile.getInputStream(entry), UTF_8);
                    break;
                }
            }
        }

        ODTReadableErrorHandler odtReadableErrorHandler = new ODTReadableErrorHandler();
        contentXml = odtReadableErrorHandler.setErrorMessage(contentXml, error);
        File tmp = new File(userTmpDir, "odterror");
        try (FileOutputStream fos = new FileOutputStream(tmp)) {
            fos.write(contentXml.getBytes(UTF_8));
            fos.flush();
        }
        try (FileSystem fs = FileSystems.newFileSystem(template.getSrc().toPath(), null)) {
            Path fileInsideZipPath = fs.getPath("/" + CONTENT_XML);
            Files.copy(tmp.toPath(), fileInsideZipPath, StandardCopyOption.REPLACE_EXISTING);
        }
        if (!tmp.delete()) {
            log.error("renderOdtError tmp.delete() failed for " + tmp.getAbsolutePath());
        }
        FileResult result = new FileResult(template);
        result.target = new File(template.getTmpDir(), "error");
        result.contentType = templateFormatter.Convert(template.getSrc(), result.target, "pdf", false);
        return result;
    }
}