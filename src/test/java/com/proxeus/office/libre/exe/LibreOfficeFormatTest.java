package com.proxeus.office.libre.exe;

import org.junit.Test;

import static com.proxeus.office.libre.exe.LibreOfficeFormat.*;
import static org.junit.Assert.*;

public class LibreOfficeFormatTest {

    @Test(expected = Exception.class)
    public void get_shouldThrowExceptionWithNull() throws Exception {
        get(null);
    }

    @Test(expected = Exception.class)
    public void get_shouldThrowExceptionWithEmptyString() throws Exception {
        get("");
    }

    @Test(expected = Exception.class)
    public void get_shouldThrowExceptionWithNonExistingFormat() throws Exception {
        get("ian");
    }

    @Test
    public void get_shouldReturnCorrectFormats() throws Exception {
        assertEquals(PDF, get("PDF"));
        assertEquals(PDF, get("pdf"));
        assertEquals(MS_DOCX, get("ms-docx"));
        assertEquals(MS_DOCX, get("ms-DOCX"));
        assertEquals(DOCX, get("oo-docx"));
        assertEquals(MS_DOCX, get("docx"));
        assertEquals(MS_DOC, get("ms-doc"));
        assertEquals(MS_DOC, get("doc"));
        assertEquals(ODT, get("odt"));
    }
}