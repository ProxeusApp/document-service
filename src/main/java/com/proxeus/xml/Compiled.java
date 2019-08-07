package com.proxeus.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


/**
 * Compiled helps to compile and replace an actual element in the XML structure to prevent from compiling the entire thing.
 * For example:
 * <a>
 *     <b>
 *         ...
 *     </b>
 *     <b>
 *         ...
 *         <c>...</c>
 *     </b>
 *     <b>
 *         <c>
 *             ...
 *             <d> <---- this is the only root node that needs to be complied
 *                 {%if%}..
 *             </d>
 *         </c>
 *         <c>
 *             ...
 *         </c>
 *     </b>
 * </a>
 *
 * By just taking <d></d> instead of the entire thing to be compiled, we increase performance a lot.
 *
 * After replacing the un-compiled element with this one, the toOutputStream method or any other output method can be used as usual.
 */
public class Compiled extends Element{
    private ByteArrayOutputStream outputStream;

    public Compiled(){
        outputStream = new ByteArrayOutputStream();
    }

    public void toOutputStream(OutputStream os, Charset charset) throws IOException {
        outputStream.writeTo(os);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    public InputStream toInputStream(Charset charset){
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    public void toString(StringBuilder sb) {
        //charset missing here just taking UTF-8 as default as I assume this would be called only for debugging purposes
        sb.append(new String(outputStream.toByteArray(), StandardCharsets.UTF_8));
    }

    public OutputStream getOutputStream(){
        return outputStream;
    }
}