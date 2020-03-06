package com.proxeus.document;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class AssetFileTest {

    private static File tmpDir;


    @BeforeClass
    public static void setTmpDir() {
        String property = "java.io.tmpdir";
        String tempDir = System.getProperty(property);
        tmpDir = new File(tempDir);
    }

    @Test
    public void testFindLocalFile() {
        String file = "/img.jpg";
        AssetFile newAssetFile = AssetFile.find(file, tmpDir);

        Assert.assertEquals(new File(String.format("%s/img.jpg", tmpDir)), newAssetFile.src);
    }

    @Test
    public void testFindEmbeddedFile() {
        String lexicalXSDBase64Binary = " iVBORw0KGgoAAAANSUhEUgAAAAUA";

        String png = String.format("data:image/png;base64,%s\n" +
                "    AAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO\n" +
                "        9TXL0Y4OHwAAAABJRU5ErkJggg==", lexicalXSDBase64Binary);


        byte[] expectedBytes = javax.xml.bind.DatatypeConverter.parseBase64Binary(lexicalXSDBase64Binary);

        AssetFile newAssetFile = AssetFile.find(png, tmpDir);


        byte[] assetBytes;
        try {
            assetBytes = Files.readAllBytes(newAssetFile.src.toPath());
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail();
            return;
        }

        Assert.assertArrayEquals(expectedBytes, assetBytes);
    }
}
