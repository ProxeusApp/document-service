package com.proxeus.document.odt;

import com.proxeus.compiler.jtwig.MyJTwigCompiler;
import com.proxeus.document.AssetFile;
import com.proxeus.document.Template;
import com.proxeus.document.odt.img.ImageAdjusterRunnable;
import com.proxeus.document.odt.img.ImageSettings;
import com.proxeus.error.CompilationException;
import com.proxeus.util.Eval;
import com.proxeus.util.zip.EntryFileFilter;
import com.proxeus.util.zip.Zip;
import com.proxeus.xml.Config;
import com.proxeus.xml.Element;
import com.proxeus.xml.Node;
import com.proxeus.xml.Tag;
import com.proxeus.xml.TagType;
import com.proxeus.xml.VarParser;
import com.proxeus.xml.XmlTemplateHandler;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * ODTContext takes away the complexity from ODTCompiler and the image handling
 * by keeping the things that are needed from the beginning until the end here together.
 */
public class ODTContext {
    public final static String CONTENT_XML = "content.xml";
    public final static String STYLE_XML = "styles.xml";
    public final static String META_XML = "meta.xml";
    public Template template;
    public ExecutorService imgExecutor;
    public ExecutorService compileExecutor;
    public VarParser varParser;
    public boolean extractedFonts;
    public XmlTemplateHandler manifest = null;
    public File manifestDest = null;
    public List<File> extractedFiles = new ArrayList<>(2);
    public Queue<AssetFile> assetFilesToInclude = new ConcurrentLinkedQueue<>();
    public Queue<String> fileToRemoveFromZip = new ConcurrentLinkedQueue<>();
    public Queue<Exception> compileExceptions = new ConcurrentLinkedQueue<>();

    public ODTContext(Template template, VarParser varParser) {
        this.template = template;
        this.varParser = varParser;
        if (!readVarsOnly()) {
            imgExecutor = Executors.newFixedThreadPool(4);
            compileExecutor = Executors.newFixedThreadPool(2);
        }
    }

    public void extractAndCompile(Config conf, MyJTwigCompiler compiler) throws Exception {
        Map<String, Boolean> dirsMade = new HashMap<>(4);
        Zip.extract(template.src, new EntryFileFilter() {
            public void next(ZipEntry entry, ZipFile zf) throws Exception {
                if (entry.getName().startsWith("Fonts/")) {
                    extractedFonts = true;
                    File toExtract = new File(template.tmpDir, entry.getName());
                    ensureDirExists(toExtract.getParentFile(), dirsMade);
                    FileUtils.copyToFile(zf.getInputStream(entry), toExtract);
                } else if (!readVarsOnly() && entry.getName().equals("META-INF/manifest.xml")) {
                    manifest = new XmlTemplateHandler(conf, zf.getInputStream(entry), entry.getSize());
                    manifestDest = new File(template.tmpDir, entry.getName());
                    ensureDirExists(manifestDest.getParentFile(), dirsMade);
                } else if (entry.getName().endsWith(CONTENT_XML) || entry.getName().endsWith(STYLE_XML)) {
                    File toExtract = null;
                    if (!readVarsOnly()) {
                        toExtract = new File(template.tmpDir, entry.getName());
                        ensureDirExists(toExtract.getParentFile(), dirsMade);
                    }
                    XmlTemplateHandler xml = new XmlTemplateHandler(conf, zf.getInputStream(entry), entry.getSize());
                    try {
                        checkForAssetFileReplacements(xml, entry);
                    } catch (Exception e) {
                        throw new CompilationException("error when replacing images", e);
                    }
                    if (readVarsOnly()) {
                        xml.findVars(varParser);
                        xml.free();
                    } else {
                        extractedFiles.add(toExtract);
                        compileExecutor.submit(new ODTCompileRunnable(compiler, toExtract, xml, template.getDataCopy(), compileExceptions));
                    }
                }
            }
        });
        waitForCompileTasksToFinish();
    }

    public File getFontsDir(){
        return new File(template.tmpDir, "Fonts");
    }

    private void ensureDirExists(File dir, Map<String, Boolean> dirsMade){
        if(!dirsMade.containsKey(dir.getAbsolutePath())){
            dir.mkdirs();
            dirsMade.put(dir.getAbsolutePath(), true);
        }
    }

    private void waitForCompileTasksToFinish() throws Exception {
        if (!readVarsOnly()) {
            compileExecutor.shutdown();
            compileExecutor.awaitTermination(15, TimeUnit.SECONDS);
            if(compileExceptions.size()>0){
                //at least one exception was thrown during compilation, throw the first one
                //lets assume the first exception is accurate enough to help solving the issue
                throw compileExceptions.poll();
            }
        }
    }

    public void waitForImageTasksToFinish() throws Exception {
        if (!readVarsOnly()) {
            imgExecutor.shutdown();
            imgExecutor.awaitTermination(15, TimeUnit.SECONDS);
        }
    }

    protected void processManifest() {
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
    }

    public void finish() throws Exception {
        Set<String> collectedEmbeddedObjects = new HashSet<>();
        try (FileSystem fs = FileSystems.newFileSystem(template.src.toPath(), null)) {
            for (File newPath : extractedFiles) {
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
            waitForImageTasksToFinish();
            processManifest();
            insertManifest(fs);
            includeAssets(fs);
        }
    }

    private void insertManifest(FileSystem fs) {
        try {
            Files.move(manifestDest.toPath(), fs.getPath(getZipPath(template.tmpDir, manifestDest.getAbsolutePath())), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            //not important as it will work without it
        }
    }

    private void includeAssets(FileSystem fs) throws IOException {
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
     * styles:
     * <p>
     * <draw:frame draw:style-name="Mfr1" draw:name="Image2" text:anchor-type="paragraph" svg:x="-0.2563in" svg:y="-0.4437in" svg:width="1.428in" svg:height="0.7575in" draw:z-index="7">
     * <draw:image xlink:href="../native_americans_maps.jpg" xlink:type="simple" xlink:show="embed" xlink:actuate="onLoad" draw:filter-name="&lt;All formats&gt;" loext:mime-type="image/jpeg"/>
     * </draw:frame>
     * <p>
     * <p>
     * <draw:frame draw:style-name="fr1" draw:name="{{input.ImageFile2}}" text:anchor-type="paragraph" svg:x="0.0138in"
     * svg:y="0.0409in" svg:width="2.7709in" svg:height="2.328in" draw:z-index="1">
     * <draw:image xlink:href="Pictures/100000000000018F00000113DD14385317835C26.png" xlink:type="simple" xlink:show="embed"
     * xlink:actuate="onLoad" loext:mime-type="image/png"/>
     * </draw:frame>
     */
    private void checkForAssetFileReplacements(XmlTemplateHandler xml, ZipEntry entry) throws Exception {
        String xmlDirPath = Zip.dirPath(entry.getName());
        List<Element> imgElements = xml.findElementsByName("draw:frame");
        if (imgElements != null && imgElements.size() > 0) {
            for (Element imgEle : imgElements) {
                List<Element> drwImgs = imgEle.findElementByName("draw:image");
                if (drwImgs != null && drwImgs.size() > 0) {
                    Element drwImg = drwImgs.get(0);
                    assetFileReplacement(imgEle, drwImg, xmlDirPath);
                }
            }
        }
    }

    /**
     * Do the necessary changes on the ODT's XML and execute the image adjuster thread.
     * If we are not just looking for vars and if a valid var was specified in the ODT for this very img element.
     *
     * @param imgEle img element
     * @param drwImg child element of img element
     * @param xmlDirPath inside the zip
     */
    public void assetFileReplacement(Element imgEle, Element drwImg, String xmlDirPath) throws Exception {
        String varWithOptions = imgEle.attr("draw:name");
        if (varWithOptions == null || !(varWithOptions = varWithOptions.trim()).startsWith("{{") || !varWithOptions.endsWith("}}")) {
            //continue as there is no valid var expression and therefore nothing for us to do on this image tag
            return;
        }
        //take away the expression >{{< * >}}<
        varWithOptions = varWithOptions.substring(2, varWithOptions.length() - 2).trim();
        ImageSettings imgStngs = new ImageSettings(
                xmlDirPath,
                drwImg.attr("xlink:href"),
                varWithOptions,
                imgEle.attr("svg:width"),
                imgEle.attr("svg:height"),
                this
        );
        if (readVarsOnly()) {
            //we want to read the var only so parse it and continue
            //it is important to do it after new ImageSettings to parse away the image options
            varParser.Parse("{{"+imgStngs.varOnly+"}}");
            return;
        }
        imgEle.attr("draw:name", "img" + System.nanoTime());

        if (imgStngs.readyToBeExecuted()) {
            imgStngs.touchFile();
            //resolve the variable synchronously to the image settings so it can be executed asynchronously
            imgStngs.localRemoteOrEmbeddedFileObject = Eval.me(imgStngs.varOnly, template.data);
            if(imgStngs.localRemoteOrEmbeddedFileObject != null){
                //the adjusted image will be always a png
                drwImg.attr("loext:mime-type", "image/png");
                //make sure it is embedded by forcing the path Pictures/imageSettingsID
                //this path must be relative
                //if there is an embedded object like Object 1/Pictures.., the path in the content.xml is still Pictures/.. but not in the root manifest
                drwImg.attr("xlink:href", "Pictures/" + imgStngs.ID());
                imgExecutor.submit(new ImageAdjusterRunnable(imgStngs));
            }
        }
    }

    public boolean readVarsOnly() {
        return varParser != null;
    }
}