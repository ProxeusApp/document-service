package com.proxeus.document;

import org.apache.log4j.Logger;

import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

/**
 * Installs fonts on a Linux system.
 */
public class FontInstaller {
    private Logger log = Logger.getLogger(this.getClass());

    private File userFontsDir;
    private int lastFontCount = 0;
    private int currentFontCount = 0;

    public FontInstaller() {
        userFontsDir = obtainUserFontsDir();
        updateFontCount();
        CmdforceFcCache();
        lastFontCount = currentFontCount;
    }

    private void updateFontCount() {
        File[] f = userFontsDir.listFiles();
        if (f != null) {
            currentFontCount = f.length;
        }
    }

    public static File obtainUserFontsDir() {
        File fontsDir = new File(System.getProperty("user.home"));
        if (fontsDir.getAbsolutePath().compareToIgnoreCase("/root") == 0 ||
                fontsDir.getAbsolutePath().compareToIgnoreCase("/document-service/dockerhome") == 0 ||
                !fontsDir.exists()) {// user without home folder or Docker
            fontsDir = new File("/document-service/fonts");
        } else {
            fontsDir = new File(new File(System.getProperty("user.home")), ".fonts");
        }
        if (!fontsDir.exists()) {
            fontsDir.mkdir();
        }
        return fontsDir;
    }

    /**
     * @param path
     * @return true if fonts were installed
     */
    public boolean installDir(File path) {
        updateFontCount();
        File[] files = path.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    installDir(f);
                } else {
                    instFile(f);
                }
            }
            return updateSystemCache();
        }
        return false;
    }

    /**
     * @param path
     * @return true if font was installed
     */
    public boolean installFile(File path) {
        updateFontCount();
        instFile(path);
        return updateSystemCache();
    }

    private boolean updateSystemCache() {
        updateFontCount();
        if (currentFontCount != lastFontCount) {
            lastFontCount = currentFontCount;
            CmdforceFcCache();
            return true;
        }
        return false;
    }

    private void instFile(File path) {
        try {
            String n = path.getName();
            n = n.substring(n.lastIndexOf('.'));
            n = readFontName(path)/*+"_"+path.lastModified()+"_"+path.length()*/ + n;
            Files.copy(path.toPath(), new File(userFontsDir, n).toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String readFontName(File path) {
        try (FileInputStream fis = new FileInputStream(path)) {
            Font f = Font.createFont(Font.TRUETYPE_FONT, fis);
            return f.getPSName();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private void CmdforceFcCache(){
        try {
            Runtime.getRuntime().exec("fc-cache -f " + userFontsDir.getAbsolutePath()).waitFor(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.info("Cannot execute fc-cache -f");
        }
    }
}
