package com.proxeus.util.zip;

import java.io.InputStream;
import java.util.zip.ZipEntry;

public interface EntryFilter {
    void next(ZipEntry current, InputStream inputStream) throws Exception;
}