package com.proxeus.document.odt;

import com.proxeus.compiler.jtwig.MyJTwigCompiler;
import com.proxeus.document.AssetFile;
import com.proxeus.document.FileResult;
import com.proxeus.document.FontInstaller;
import com.proxeus.document.Template;
import com.proxeus.document.odt.img.ImageAdjustProcessorFactory;
import com.proxeus.error.CompilationException;
import com.proxeus.error.InternalException;
import com.proxeus.error.UnavailableException;
import com.proxeus.office.libre.LibreOfficeAssistant;
import com.proxeus.util.zip.EntryFileFilter;
import com.proxeus.util.zip.Zip;
import com.proxeus.xml.Config;
import com.proxeus.xml.template.TemplateHandler;
import com.proxeus.xml.template.TemplateHandlerFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

    final static String CONTENT_XML = "content.xml";
    final static String STYLE_XML = "styles.xml";
    public final static String META_XML = "meta.xml";
    public Template template;
    private boolean extractedFonts;
    private TemplateHandler manifest = null;
    private File manifestDest = null;
    private Queue<String> fileToRemoveFromZip = new ConcurrentLinkedQueue<>();


    private ImageAdjustProcessorFactory imageAdjuster;
    private TemplateHandlerFactory templateHandlerFactory;

    private LibreOfficeAssistant libreOfficeAssistant;

    ODTRenderer(Template template, TemplateHandlerFactory templateHandlerFactory, LibreOfficeAssistant libreOfficeAssistant) {
        this.template = template;
        this.templateHandlerFactory = templateHandlerFactory;
        this.libreOfficeAssistant = libreOfficeAssistant;
        this.imageAdjuster = new ImageAdjustProcessorFactory(template.tmpDir, template.getDataCopy());
    }


    protected FileResult compile(Config conf, MyJTwigCompiler compiler, FontInstaller fontInstaller) throws Exception {
        try {
            List<File> extractedFiles = extractAndCompile(conf, compiler);
            assembleZipFile(extractedFiles);
        } catch (CompilationException e) {
            if (template.embedError) {
                return renderOdtError(template, e.getMessage(), template.tmpDir);
            } else {
                throw e;
            }
        }
        return convertToformat(fontInstaller);
    }


    private List<File> extractAndCompile(Config conf, MyJTwigCompiler compiler) throws Exception {
        log.debug(String.format("DEBUG TEMPLATE %s\n", template));

        ExecutorService compileExecutor = Executors.newFixedThreadPool(2);
        List<File> extractedFiles = new ArrayList<>(2);
        Queue<Exception> compileExceptions = new ConcurrentLinkedQueue<>();
        try {
            Zip.extract(template.src, new EntryFileFilter() {
                public void next(ZipEntry entry, ZipFile zf) throws Exception {
                    if (entry.getName().startsWith("Fonts/")) {
                        extractedFonts = true;
                        File toExtract = new File(template.tmpDir, entry.getName());
                        FileUtils.copyToFile(zf.getInputStream(entry), toExtract);
                    } else if (entry.getName().equals("META-INF/manifest.xml")) {
                        manifest = templateHandlerFactory.newInstance();
                        manifest.process(zf.getInputStream(entry));
                        manifestDest = new File(template.tmpDir, entry.getName());
                    } else if (entry.getName().endsWith(CONTENT_XML) || entry.getName().endsWith(STYLE_XML)) {
                        TemplateHandler xml = templateHandlerFactory.newInstance(
                                imageAdjuster.newInstance(entry.getName())
                        );
                        xml.process(zf.getInputStream(entry));


                        File toExtract = new File(template.tmpDir, entry.getName());
                        log.debug(String.format("DEBUG TO EXTRACT %s\n", toExtract));
                        extractedFiles.add(toExtract);

                        FileOutputStream output = new FileOutputStream(toExtract);
                        compileExecutor.submit(() -> {
                            try {
                                xml.render(output, template.getDataCopy());
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
        log.debug(String.format("DEBUG FINISH FILESYSTEM PATH %s\n", template.src.toPath()));
        try (FileSystem fs = FileSystems.newFileSystem(template.src.toPath(), null)) {
            for (File newPath : extractedFiles) {
                log.debug(String.format("DEBUG FINISH EXTRACTED FILE %s\n", newPath));
                String zipPath = getZipPath(template.tmpDir, newPath.getAbsolutePath());
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

    private FileResult convertToformat(FontInstaller fontInstaller) throws UnavailableException {
        try {
            FileResult result = new FileResult(template);
            result.target = new File(template.tmpDir, "final");
            result.template = template;
            boolean newFontsInstalled = extractedFonts && fontInstaller.installDir(getFontsDir());
            result.contentType = libreOfficeAssistant.Convert(template.src, result.target, template.format, newFontsInstalled);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new UnavailableException("LibreOffice error during convert to " + template.format + ": " + e.getMessage());
        }
    }

    File getFontsDir() {
        return new File(template.tmpDir, "Fonts");
    }

    Queue<AssetFile> waitForImageTasksToFinish() throws Exception {
        ImageAdjustProcessorFactory.Result result = imageAdjuster.finish();
        if (result.getExceptions().size() > 0) {
            throw result.getExceptions().poll();
        }
        return result.getAssetFiles();
    }

    private void processManifest(Queue<AssetFile> assetFiles) {
        // TODO: Implement
        /*
        if (manifest != null) {
            try {
                List<Element> manifestFileEntries = manifest.findElementsByName("manifest:file-entry");
                ListIterator<Element> elementListIterator = manifestFileEntries.listIterator();
                for (AssetFile f : assetFilesToInclude) {
                    if (f.orgZipPath != null) {
                        while (elementListIterator.hasNext()) {
                            Element element = elementListIterator.next();
                            if (element.toString().contains(f.orgZipPath)) {
                                element.remove();//remove from dom
                                elementListIterator.remove();//remove from this list
                                break;
                            }
                        }
                    }
                }
                List<Element> manifestMain = manifest.findElementsByName("manifest:manifest");
                if (manifestMain != null && manifestMain.size() > 0) {
                    for (AssetFile f : assetFilesToInclude) {
                        String newImgTag = "<manifest:file-entry manifest:full-path=\"" + f.newZipPath.substring(1) + "\" manifest:media-type=\"image/png\"/>";
                        ((Node) manifestMain.get(0)).addChild(new Node(new Tag(newImgTag, TagType.START_AND_END)));
                    }
                }
                FileOutputStream manifestOs = new FileOutputStream(manifestDest);
                manifest.toOutputStream(manifestOs);
                manifestOs.flush();
                manifestOs.close();
                manifest.free();
                manifest = null;
            } catch (Exception e) {
                //not important as libre will handle it with the repair feature
                System.err.println("couldn't modify the manifest");
            }
        }

         */
    }


    private void insertManifest(FileSystem fs) {
        try {
            Files.move(manifestDest.toPath(), fs.getPath(getZipPath(template.tmpDir, manifestDest.getAbsolutePath())), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            //not important as it will work without it
        }
    }

    private void includeAssets(Queue<AssetFile> assetFilesToInclude, FileSystem fs) throws IOException {
        for (AssetFile f : assetFilesToInclude) {
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

        ODTReadableErrorHandler odtReadableErrorHandler = new ODTReadableErrorHandler();
        contentXml = odtReadableErrorHandler.setErrorMessage(contentXml, error);
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