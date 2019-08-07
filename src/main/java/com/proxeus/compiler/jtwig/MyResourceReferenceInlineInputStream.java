package com.proxeus.compiler.jtwig;

import org.jtwig.resource.reference.ResourceReference;

import java.io.InputStream;

public class MyResourceReferenceInlineInputStream extends ResourceReference {
    public InputStream inputStream;
    public final static String INLINE_INPUT_STREAM = "_ii_";
    private boolean alreadyLoaded = false;
    public MyResourceReferenceInlineInputStream(InputStream inputStream) {
        super(INLINE_INPUT_STREAM, "");
        this.inputStream = inputStream;
    }

    public InputStream getInputStream(){
        if(alreadyLoaded){
            //for self import it is being called again
            try{
                inputStream.reset();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        alreadyLoaded = true;
        return inputStream;
    }
}
