package com.proxeus.util;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * Image helps us to create a container in which it places the actual image as configured with the parameters.
 * The container will be invisible as the output is always png.
 */
public class Image {
    public static BufferedImage resize(BufferedImage inputImage, int scaledWidth, int scaledHeight)
           throws IOException {
        // creates output image
        BufferedImage outputImage = new BufferedImage(scaledWidth,
                scaledHeight, inputImage.getType());

        // scales the input image to the output image
        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(inputImage, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();

        return outputImage;
    }

    public static void adjustToFitToRatio(File src, File dst, int maxWidth, int maxHeight, String alignment, boolean sizeItUpIfSmaller) throws IOException {
        BufferedImage image = ImageIO.read(src);

        int width = image.getWidth();
        int height = image.getHeight();
        if (width == 0 || height == 0) {
          throw new IOException("Image has width or height zero");
        }

        // TODO: fixme
        /*
        if (sizeItUpIfSmaller) {
          if (width < height) {
            double w = (double) image.getHeight() * (double) maxWidth / (double) maxHeight;
            image = resize(image, (int) w, maxHeight);
          } else {
            double h = (double) image.getWidth() * (double) maxHeight / (double) maxWidth;
            image = resize(image, maxWidth, (int) h);
          }
        }
        width = image.getWidth();
        height = image.getHeight();
        */

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
            resize(image, outputWidth, outputHeight);
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
        BufferedImage containerImage = resize(image, width, height);

        // BufferedImage containerImage = new BufferedImage(maxWidth, maxHeight);
        // containerImage.setOutputQuality(98.0);
        // containerImage.addImage(image, width, height, false);
        ImageIO.write(containerImage, "png", dst);
    }
}
