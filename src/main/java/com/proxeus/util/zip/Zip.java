package com.proxeus.util.zip;

import com.proxeus.SparkServer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Zip process and pack helper.
 */
public class Zip {
    public static void extract(InputStream zipStream, EntryFilter ef) throws Exception {
        try (ZipInputStream zipInputStream = new ZipInputStream(zipStream)) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                ef.next(zipEntry, zipInputStream);
                zipInputStream.closeEntry();
            }
        }
    }

    public static void extract(File src, EntryFileFilter ef) throws Exception{
        try (ZipFile zipFile = new ZipFile(src)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ef.next(entries.nextElement(), zipFile);
            }
        }
    }

    public static File resourceDir(String tmpDir, String resourceDirPath) throws Exception {
        File f = new File(tmpDir, resourceDirPath);
        File[] newFileEntries = null;
        final File jarFile = new File(Zip.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        if(jarFile.isFile()) {  // Run with JAR file
            List<File> fentries = new ArrayList<>(3);
            final JarFile jar = new JarFile(jarFile);
            final Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
            while(entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                final String n = entry.getName();
                if (n.matches(resourceDirPath + "\\/.+")) { //filter according to the path
                    File newFile = Paths.get(tmpDir, entry.getName()).toFile();
                    newFile.getParentFile().mkdirs();
                    try(FileOutputStream fis = new FileOutputStream(newFile)){
                        IOUtils.copy(jar.getInputStream(entry), fis);
                    }
                    fentries.add(newFile);
                }
            }
            newFileEntries = fentries.toArray(new File[]{});
            jar.close();
        } else { // Run with IDE
            final URL url = SparkServer.class.getResource("/" + resourceDirPath);
            if (url != null) {
                try {
                    final File apps = new File(url.toURI());
                    newFileEntries = apps.listFiles();
                } catch (URISyntaxException ex) {}
            }
        }
        File zip = new File(tmpDir, resourceDirPath+".zip");
        if(newFileEntries == null){
            return null;
        }
        Zip.pack(zip, newFileEntries);
        FileUtils.deleteDirectory(f);
        return zip;
    }

    public static void pack(File zip, File[] entries) throws Exception {
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip));
        for(File entry : entries){
            ZipEntry e = new ZipEntry(entry.getName());
            out.putNextEntry(e);
            try(FileInputStream fis = new FileInputStream(entry)) {
                IOUtils.copy(fis, out);
            }
            out.closeEntry();
        }
        out.close();
    }

    public static File zipEntryToFile(ZipEntry zipEntry, InputStream inputStream, File tmpDir, String fname) throws IOException {
        File file = new File(tmpDir, fname);
        if (zipEntry.isDirectory()) {
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    throw new IOException("Could not process asset(image?) from ZIP:" + file.getAbsolutePath());
                }
            }
        } else {
            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                IOUtils.copy(inputStream, fileOutputStream);
                fileOutputStream.flush();
            }
        }
        return file;
    }

    public static void delete(Path path) {
        try {
            Files.delete(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String dirPath(String zipFilePath){
        if(zipFilePath.contains("/")){
            return zipFilePath.substring(0, zipFilePath.lastIndexOf("/"));
        }
        //else return root
        return "";
    }
}