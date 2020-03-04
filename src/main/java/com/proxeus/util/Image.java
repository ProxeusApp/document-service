package com.proxeus.util;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * Image helps us to create a container in which it places the actual image as configured with the parameters.
 * The container will be invisible as the output is always png.
 */
public class Image {
    public static void adjustToFitToRatio(File src, File dst, int maxWidth, int maxHeight, String alignment, boolean sizeItUpIfSmaller) throws IOException {
        javaxt.io.Image image = new javaxt.io.Image(src);
        image.setOutputQuality(98.0);
        if (sizeItUpIfSmaller) {
            if (maxHeight > maxWidth) {
                image.setHeight(maxHeight);//works with lower images
            } else {
                image.setWidth(maxWidth);
            }
        }
        int width = image.getWidth();
        int height = image.getHeight();

        double outputImage = 0.0D;
        if (maxWidth < maxHeight) {
            outputImage = (double) maxWidth / (double) width;
        } else {
            outputImage = (double) maxHeight / (double) height;
        }

        double g2d = (double) width * outputImage;
        double dh = (double) height * outputImage;
        int outputWidth = (int) Math.round(g2d);
        int outputHeight = (int) Math.round(dh);
        if (outputWidth > width || outputHeight > height) {
            outputWidth = width;
            outputHeight = height;
        }

        if (image.getWidth() != outputWidth || image.getHeight() != outputHeight) {
            image.resize(outputWidth, outputHeight, false);
        }
        width = image.getWidth();
        height = image.getHeight();

        alignment = alignment.toLowerCase();

        if (alignment.contains("top")) {
            height = 0;
        } else if (alignment.contains("bottom")) {
            if (maxHeight > height) {
                height = (maxHeight - height);
            } else {
                height = (height - maxHeight);
            }
        } else {
            if (maxHeight > height) {
                height = (maxHeight - height) / 2;
            } else {
                height = 0;
            }
        }

        if (alignment.contains("left")) {
            width = 0;
        } else if (alignment.contains("right")) {
            if (maxWidth > width) {
                width = (maxWidth - width);
            } else {
                width = (width - maxWidth);
            }
        } else {
            if (maxWidth > width) {
                width = (maxWidth - width) / 2;
            } else {
                width = 0;
            }
        }
        javaxt.io.Image containerImage = new javaxt.io.Image(maxWidth, maxHeight);
        containerImage.setOutputQuality(98.0);
        containerImage.addImage(image, width, height, false);
        ImageIO.write(containerImage.getBufferedImage(), "png", dst);
    }
}
