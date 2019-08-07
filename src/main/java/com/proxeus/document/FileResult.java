package com.proxeus.document;

import java.io.File;

/**
 * FileResult helps to deliver the results and free resources as soon as they are not needed anymore.
 */
public class FileResult {
    public File target;
    public String contentType;
    public Template template;

    public FileResult(Template template){
        this.template = template;
    }

    public void release(){
        if(template!=null){
            template.release();
        }else{
            if(!target.delete()){
                System.err.println("error when deleting directory: "+ target.getAbsolutePath());
            }
        }
        target = null;
        contentType = null;
        template = null;
    }
}


