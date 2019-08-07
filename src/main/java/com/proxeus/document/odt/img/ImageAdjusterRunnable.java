package com.proxeus.document.odt.img;

import com.proxeus.document.AssetFile;
import com.proxeus.util.Image;

/**
 * ImageAdjusterRunnable helps us to gain performance with the image manipulations as it can run independently from the main thread.
 * Until the main thread is ready to pack the files back to the ODT.
 */
public class ImageAdjusterRunnable implements Runnable {
    private ImageSettings imageSettings;

    public ImageAdjusterRunnable(ImageSettings imageSettings) {
        this.imageSettings = imageSettings;
    }

    public void run() {
        try {
            AssetFile assetFile = AssetFile.find(imageSettings.localRemoteOrEmbeddedFileObject, imageSettings.cfc.template.tmpDir);
            if (assetFile != null) {
                assetFile.dst = imageSettings.dst;
                assetFile.orgZipPath = imageSettings.refFileName;
                assetFile.newZipPath = "/" + joinZipPath(imageSettings.xmlDirPath, "Pictures/" + imageSettings.ID());
                if(imageSettings.cfc.assetFilesToInclude.offer(assetFile)){
                    Image.adjustToFitToRatio(
                            assetFile.src,
                            imageSettings.dst,
                            imageSettings.containerWidth,
                            imageSettings.containerHeight,
                            imageSettings.align,
                            imageSettings.sizeItUpIfSmaller
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("couldn't adjust the image to the provided container");
            e.printStackTrace();
        }
    }

    private static String joinZipPath(String a, String b) {
        if (a.length() == 0) {
            return b;
        }
        return a + "/" + b;
    }
}