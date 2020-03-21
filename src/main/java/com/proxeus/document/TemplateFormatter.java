package com.proxeus.document;

import com.proxeus.office.libre.exe.Extension;

import java.io.File;

public interface TemplateFormatter {
    String Convert(File src, File dst, String format, boolean restart) throws Exception;
    Extension getExtension(String os);
}
