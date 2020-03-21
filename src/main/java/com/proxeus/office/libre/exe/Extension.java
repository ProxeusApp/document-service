package com.proxeus.office.libre.exe;

import java.io.InputStream;

/**
 * A helper to return the LibreOffice extension from LibreOfficeAssistance.
 */
public class Extension {
    private InputStream inputStream;
    private String fileName;
    private String contentType;

    public Extension(String fileName, String contentType) {
        this.fileName = fileName;
        this.contentType = contentType;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
