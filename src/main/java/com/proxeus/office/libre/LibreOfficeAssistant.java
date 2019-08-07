package com.proxeus.office.libre;

import com.proxeus.error.UnavailableException;
import com.proxeus.office.libre.exe.Extension;
import com.proxeus.office.libre.exe.LibreOffice;
import com.proxeus.office.libre.exe.LibreOfficePool;

import java.io.Closeable;
import java.io.File;
import java.io.InputStream;

/**
 * LibreOfficeAssistant makes the communication between LibreOffice and the Document-Service easier and safely.
 */
public class LibreOfficeAssistant implements Closeable {
    private LibreOfficePool libreOfficePool;

    public LibreOfficeAssistant(LibreConfig libreConfig) throws Exception {
        if(libreConfig == null){
            libreConfig = new LibreConfig();
        }
        libreOfficePool = new LibreOfficePool(libreConfig);
        final Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
            libreOfficePool.close();
            try{
                mainThread.join();
            }catch (Exception e){}
            }
        });
    }

    /**
     * Convert src as the provided format at dst.
     * @param src srouce file
     * @param dst destination file
     * @param format pdf, odt, docx or doc
     * @return contentType
     */
    public String Convert(File src, File dst, String format) throws Exception {
        return Convert(src, dst, format, false);
    }

    /**
     * Convert src as the provided format at dst.
     * @param src srouce file
     * @param dst destination file
     * @param format pdf, odt, docx or doc
     * @param newFontsInstalled signal new fonts are installed, to restart the executables so the font can be used
     * @return contentType
     */
    public String Convert(File src, File dst, String format, boolean newFontsInstalled) throws Exception {
        int count = 0;
        do{
            try{
                LibreOffice lo = libreOfficePool.take(newFontsInstalled);
                newFontsInstalled = false;
                return lo.Convert(src, dst, format);
            }catch(ExceptionInInitializerError wiie){
                ++count;
            }catch(Exception e){
                throw new UnavailableException("Please try again later.", e);
            }finally {
                libreOfficePool.release();
            }
        }while(count < 10);
        throw new UnavailableException("Please try again later.");
    }

    /**
     * Get the native LibreOffice Template-Assistance extension
     * @param os linux_x86_64 | mac_x86_64 | win_x86 | win_x86_64
     * @return the extension containing inputStream, contentType and fileName
     */
    public Extension getExtension(String os){
        try {
            Extension ext = new Extension();
            ext.contentType = "application/octet-stream";
            ext.fileName = "ProxeusTemplateAssistance_" + os + ".oxt";
            InputStream fis = LibreOfficeAssistant.class.getResourceAsStream("/" + ext.fileName);
            if (fis == null) {
                return null;
            }
            ext.inputStream = fis;
            return ext;
        } catch (Exception e) {}
        return null;
    }

    public void close() {
        libreOfficePool.close();
    }
}
