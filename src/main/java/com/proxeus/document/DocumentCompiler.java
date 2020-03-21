package com.proxeus.document;

import java.util.Set;

/**
 * Currently this interface could be avoided but so far, it defines the interface as needed.
 * If other formats based on XML are going to be implemented, they can simply use this
 * interface to communicate with the underlying format specific compiler.
 */
public interface DocumentCompiler {
    FileResult Compile(Template template) throws Exception;
    Set<String> Vars(Template template, String varPrefix) throws Exception;
}