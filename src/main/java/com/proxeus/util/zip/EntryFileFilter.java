package com.proxeus.util.zip;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public interface EntryFileFilter {
    void next(ZipEntry current, ZipFile zipFile) throws Exception;
}