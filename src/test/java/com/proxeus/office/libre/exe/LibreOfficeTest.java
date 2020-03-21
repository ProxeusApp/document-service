package com.proxeus.office.libre.exe;

import org.junit.Test;

import static org.junit.Assert.*;

public class LibreOfficeTest {

    @Test
    public void isNumber() {
        assertTrue(LibreOffice.isNumber(new StringBuffer("5")));
        assertTrue(LibreOffice.isNumber(new StringBuffer("54545689458694589846689945868948684968496488649865948646")));
        assertFalse(LibreOffice.isNumber(new StringBuffer("abc")));
    }
}