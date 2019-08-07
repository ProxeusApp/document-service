package com.proxeus.document;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Template simplifies the compile interface.
 */
public class Template {
    public TemplateType type;
    public File src;
    public Map<String, Object> data;
    public File tmpDir;
    public String format;
    public boolean embedError;

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
        String cacheDir = System.getProperty("document.template.cache.dir");
        if(cacheDir == null || cacheDir.isEmpty()){
            cacheDir = System.getProperty("java.io.tmpdir");
        }
        tmpDir = new File(cacheDir, UUID.randomUUID().toString());
        ensureDirs();
    }

    public void ensureDirs() throws Exception {
        if (!tmpDir.exists() && !tmpDir.mkdirs()) {
            throw new Exception("couldn't create cache dir" + tmpDir.getAbsolutePath());
        }
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
}


