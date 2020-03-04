package com.proxeus.document;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.File;
import java.util.*;

/**
 * Template simplifies the compile interface.
 */
public class Template {
    private TemplateType type;
    private File src;
    private Map<String, Object> data;
    private File tmpDir;
    private String format;
    private boolean embedError;
    private String cacheDir = System.getProperty("document.template.cache.dir");
    private String alternateCacheDir = System.getProperty("java.io.tmpdir");


    public String toString(){
        return new ToStringBuilder(this).
                append("type", type).
                append("src", src).
                append("data", data).
                append("tmpDir", tmpDir).
                append("format", format).
                append("embedError", embedError).toString();
    }

    public Template() throws Exception {
        createCacheDir();
    }

    public Template(File src, Map<String, Object> data) throws Exception {
        this(src, data, null);
    }

    public Template(File src, Map<String, Object> data, String format) throws Exception {
        this.src = src;
        this.data = data;
        this.format = format;
        createCacheDir();
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

    public Map<String, Object> getDataCopy(){
        return copy(data);
    }

    public void release(){
        try{
            FileUtils.deleteDirectory(tmpDir);
        }catch(Exception e){
            System.err.println("error when deleting directory: "+ tmpDir.getAbsolutePath());
        }
        src = null;
        data = null;
        tmpDir = null;
    }

    private static Map<String, Object> copy(Map<String, Object> data){
        if(data == null){
            return new HashMap<>(0);
        }
        Map<String, Object> om = new HashMap<>(data.size());
        map(om, data);
        return om;
    }

    @SuppressWarnings("unchecked")
    private static void list(List newList, Collection orgList){
        for(Object o : orgList){
            if(o instanceof Map){
                Map<String, Object> orgm = ((Map)o);
                Map<String, Object> om = new HashMap<>(orgm.size());
                map(om, orgm);
            }else if(o instanceof Collection){
                Collection innerOrgList = (Collection)o;
                List list = new ArrayList(orgList.size());
                list(list, innerOrgList);
                newList.add(list);
            }else{
                newList.add(o);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void map(Map<String, Object> newMap, Map<String, Object> data){
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object o = entry.getValue();
            if(o instanceof Map){
                Map<String, Object> orgm = ((Map)o);
                Map<String, Object> om = new HashMap<>(orgm.size());
                map(om, orgm);
            }else if(o instanceof Collection){
                Collection orgList = (Collection)o;
                List list = new ArrayList(orgList.size());
                list(list, orgList);
            }
            newMap.put(entry.getKey(), o);
        }
    }

    public File getSrc() {
        return src;
    }

    public void setSrc(File src) {
        this.src = src;
    }

    public File getTmpDir() {
        return tmpDir;
    }

    public TemplateType getType() {
        return type;
    }

    public void setType(TemplateType type) {
        this.type = type;
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


