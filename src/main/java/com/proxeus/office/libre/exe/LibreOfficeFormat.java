package com.proxeus.office.libre.exe;

/**
 * LibreOfficeFormat handles the difficult FilterName property and the content type of the supported LibreOffice formats.
 */
public enum LibreOfficeFormat {
    PDF("writer_pdf_Export", "application/pdf", "pdf"),
    ODT("writer8", "application/vnd.oasis.opendocument.text", "odt"),
    DOCX("Office Open XML Text", "application/vnd.oasis.opendocument.text", "docx"),
    MS_DOCX("MS Word 2007 XML", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
    MS_DOC("MS Word 97", "application/msword", "doc");

    private final String filterName;
    private final String contentType;
    private final String ext;

    LibreOfficeFormat(String fn, String ct, String ext){
        this.filterName = fn;
        this.contentType = ct;
        this.ext = ext;
    }

    public static LibreOfficeFormat get(String format) throws Exception{
        if(format == null || format.isEmpty()){
            throw new Exception("output format missing: pdf, docx, doc, odt, oo-docx");
        }
        format = format.toLowerCase();
        if (format.equals("pdf")) {
            return PDF;
        } else if (format.matches("^(ms-)?docx$")) {
            return MS_DOCX;
        } else if (format.matches("^(ms-)?doc$")) {
            return MS_DOC;
        } else if (format.equals("oo-docx")) {
            return DOCX;
        } else if (format.equals("odt")) {
            return ODT;
        } else {
            throw new Exception(format + " is not supported");
        }
    }

    public String getFilterName() {
        return filterName;
    }

    public String getContentType() {
        return contentType;
    }

    public String getExt() {
        return ext;
    }
}