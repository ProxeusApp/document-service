package com.proxeus.document;

import com.proxeus.error.BadRequestException;
import com.proxeus.util.Json;
import com.proxeus.util.zip.EntryFilter;
import com.proxeus.util.zip.Zip;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.log4j.Logger;

import javax.servlet.http.Part;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.zip.ZipEntry;

import static com.proxeus.document.TemplateType.DOCX;
import static com.proxeus.document.TemplateType.ODT;

/**
 * Template simplifies the compile interface.
 */
public class Template {
    private Logger log = Logger.getLogger(this.getClass());

    private TemplateType type;
    private File src;
    private Map<String, Object> data;
    private File tmpDir;
    private String format;
    private boolean embedError;
    private String cacheDir = System.getProperty("document.template.cache.dir");
    private String alternateCacheDir = System.getProperty("java.io.tmpdir");


    public String toString() {
        return new ToStringBuilder(this).
                append("type", type).
                append("src", src).
                append("data", data).
                append("tmpDir", tmpDir).
                append("format", format).
                append("embedError", embedError).toString();
    }

    public Template() throws Exception {
        this.data = new HashMap<>();
        createCacheDir();
    }

    public Template(File src, Map<String, Object> data) throws Exception {
        this(src, data, null);
    }

    public Template(File src, Map<String, Object> data, String format) throws Exception {
        this.src = src;
        this.data = data;
        if (this.data == null) {
            this.data = new HashMap<>();
        }
        this.format = format;
        createCacheDir();
    }

    public static Template fromFormData(Collection<Part> parts, String format) throws Exception {
        Template template = new Template();

        if (format == null){
            format = "pdf";
        }

        Iterator<Part> it = parts.iterator();
        while (it.hasNext()) {
            Part part = it.next();
            try (InputStream inputStream = part.getInputStream()) {
                String filename = part.getSubmittedFileName();
                processEntry(template, filename, inputStream, false);
            }
        }

        template.setFormat(format);
        return template;
    }

    public static Template fromZip(InputStream zipStream, String format) throws Exception {
        Template template = new Template();
        try {
            if (format == null){
                format = "pdf";
            }
            extractZIP(template, zipStream);
            template.setFormat(format);
            return template;
        } catch (Exception e) {
            throw new BadRequestException("Please read the specification for creating the request with the zip package. zip[tmpl.odt,data.json,assets1,asset2...]");
        }
    }

    public static Template fromODT(InputStream inputStream) throws Exception {
        Template template = new Template();
        try {
            template.setSrc(new File(template.getTmpDir(), "tmpl.odt"));
            FileUtils.copyToFile(inputStream, template.getSrc());
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
     * @param template The template object to initialize
     * @param inputStream ZIP package input stream
     * @throws Exception
     */
    private static void extractZIP(Template template, InputStream zipStream) throws Exception {
        Zip.extract(zipStream, new EntryFilter() {
            public void next(ZipEntry zipEntry, InputStream zipInputStream) throws IOException {
                processEntry(template, zipEntry.getName(), zipInputStream, zipEntry.isDirectory());
            }
        });
    }

    private static void processEntry(Template template, String name, InputStream inputStream, boolean isDirectory) throws IOException {
        if (name.toLowerCase().endsWith(".odt")) {
            //found an odt template inside the zip
            template.setSrc(inputStreamToFile(inputStream, template.getTmpDir(), "tmpl.odt", isDirectory));
            if (!template.getSrc().exists() || template.getSrc().isDirectory()) {
                throw new IllegalStateException("couldn't process template odt");
            }
        } else if (name.toLowerCase().endsWith(".docx")) {
            //found a docx template inside the zip
            template.setSrc(inputStreamToFile(inputStream, template.getTmpDir(), "tmpl.docx", isDirectory));
            if (!template.getSrc().exists() || template.getSrc().isDirectory()) {
                throw new IllegalStateException("couldn't process template docx");
            }
        } else if (name.toLowerCase().endsWith(".json")) {
            //the json data the template is going to be resolved with
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            IOUtils.copy(inputStream, outputStream);
            template.setData(Json.fromJson(outputStream.toByteArray(), Map.class));
        } else {
            //other assets that should be referenced in the json data
            inputStreamToFile(inputStream, template.getTmpDir(), name, isDirectory);
        }
    }

    private static File inputStreamToFile(InputStream inputStream, File tmpDir, String fname, boolean isDirectory) throws IOException {
        File file = new File(tmpDir, fname);
        if (isDirectory) {
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    throw new IOException("Could not create: " + file.getAbsolutePath());
                }
            }
        } else {
            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                IOUtils.copy(inputStream, fileOutputStream);
                fileOutputStream.flush();
            }
        }
        return file;
    }

    private void createCacheDir() throws Exception {
        tmpDir = new File(getCacheDir(), UUID.randomUUID().toString());
        ensureDirs();
    }

    public void ensureDirs() throws Exception {
        if (!tmpDir.exists() && !tmpDir.mkdirs()) {
            throw new Exception("couldn't create cache dir" + tmpDir.getAbsolutePath());
        }
    }

    private String getCacheDir() {
        return (cacheDir == null || cacheDir.isEmpty()) ? alternateCacheDir : cacheDir;
    }

    public Map<String, Object> getDataCopy() {
        return copy(data);
    }

    public void release() {
        try {
            FileUtils.deleteDirectory(tmpDir);
        } catch (Exception e) {
            System.err.println("error when deleting directory: " + tmpDir.getAbsolutePath());
        }
        src = null;
        data = null;
        tmpDir = null;
    }

    private static Map<String, Object> copy(Map<String, Object> data) {
        if (data == null) {
            return new HashMap<>(0);
        }
        Map<String, Object> om = new HashMap<>(data.size());
        map(om, data);
        return om;
    }

    @SuppressWarnings("unchecked")
    private static void list(List newList, Collection orgList) {
        for (Object o : orgList) {
            if (o instanceof Map) {
                Map<String, Object> orgm = ((Map) o);
                Map<String, Object> om = new HashMap<>(orgm.size());
                map(om, orgm);
            } else if (o instanceof Collection) {
                Collection innerOrgList = (Collection) o;
                List list = new ArrayList(orgList.size());
                list(list, innerOrgList);
                newList.add(list);
            } else {
                newList.add(o);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void map(Map<String, Object> newMap, Map<String, Object> data) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object o = entry.getValue();
            if (o instanceof Map) {
                Map<String, Object> orgm = ((Map) o);
                Map<String, Object> om = new HashMap<>(orgm.size());
                map(om, orgm);
            } else if (o instanceof Collection) {
                Collection orgList = (Collection) o;
                List list = new ArrayList(orgList.size());
                list(list, orgList);
            }
            newMap.put(entry.getKey(), o);
        }
    }

    public File getSrc() {
        if (src == null) {
            throw new IllegalStateException("No source in Template");
        }
        return src;
    }

    public void setSrc(File src) {
        this.src = src;
    }

    public File getTmpDir() {
        return tmpDir;
    }

    public TemplateType getType() {
        switch (FilenameUtils.getExtension(src.getName())) {
            case "docx":
                return DOCX;
            default:
                return ODT;
        }
    }

    public boolean isEmbedError() {
        return embedError;
    }

    public void setEmbedError(boolean embedError) {
        this.embedError = embedError;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}


