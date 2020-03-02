package com.proxeus.document;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AssetFile makes it possible to transform a file out of:
 * 1. URL like http://myimagelink
 * 2. Base64 string like data:image/png;base64,...
 * 3. Local file on the disk
 *
 * It parses three types of data to get that information:
 * 1. As a string, by just converting the object to a string and trying the above 1, 2 and 3 if necessary.
 * 2. As an object/map like {"contentType":"image/png", "path":"filePath.."....} by searching case insensitive for the path.
 * 3. As a list, by searching string entries in the list and taking the first one that delivers an actual file by trying the above 3 options.
 *
 * It is important that this class makes sure, no one can break out from the tmpDir in case of 3. Local file path.
 */
public class AssetFile {
    public String orgZipPath;
    public String newZipPath;
    public File src;
    public File dst;

    public static AssetFile find(Object localRemoteOrEmbeddedFileObject, File tmpDir){
        try{
            if(localRemoteOrEmbeddedFileObject != null){
                if(localRemoteOrEmbeddedFileObject instanceof String){
                    return provideImageFileWithExtIfPossible((String)localRemoteOrEmbeddedFileObject, tmpDir);
                }else if(localRemoteOrEmbeddedFileObject instanceof Map){
                    Map m = ((Map)localRemoteOrEmbeddedFileObject);
                    for(Object k : m.keySet()){
                        //try to find the path in the map
                        ////{contentType=image/jpeg, name=IMG_20190520_195031_1.jpg, path=1643fc69-d786-45ee-b865-0259c1c4a5d4, ref=, size=3718863}
                        if("path".equals(k.toString().toLowerCase())){
                            return provideImageFileWithExtIfPossible((String)m.get(k), tmpDir);
                        }
                    }
                }else if(localRemoteOrEmbeddedFileObject instanceof Collection) {
                    //it is a list, try out the entries, the first that provides an AssetFile, will be taken
                    Collection fileList = (Collection)localRemoteOrEmbeddedFileObject;
                    AssetFile res = null;
                    for(Object val : fileList){
                        if(val != null && val instanceof String){
                            res = provideImageFileWithExtIfPossible((String) val, tmpDir);
                            if(res!=null){
                                return res;
                            }
                        }
                    }
                }
            }
        }catch (Exception dontCare){
            //happens if we weren't able to read a local, remote or embedded file out of the provided object
            System.err.println(dontCare.getMessage());
        }
        return null;
    }

    private final static Pattern embeddedFileReg = Pattern.compile("^data\\:(image\\/\\w+);base64,(.*)");
    /**
     * Provide image file with ext if possible by the input of a local, remote or embedded file.
     *
     * @param localOrRemotePath local: "file:/..." remote: "http:/..." embedded: "data:image...;base64"
     * @return asset file or null
     */
    private static AssetFile provideImageFileWithExtIfPossible(String localOrRemotePath, File cacheDir) {
        if (localOrRemotePath == null || localOrRemotePath.trim().equals("")) {
            return null;
        }
        Matcher matcher;
        if ((matcher = embeddedFileReg.matcher(localOrRemotePath)).find()) {
            try {
                byte[] imageBytes = javax.xml.bind.DatatypeConverter.parseBase64Binary(matcher.group(2));
                File localFile = new File(cacheDir, UUID.randomUUID().toString());
                try (FileOutputStream fos = new FileOutputStream(localFile)) {
                    fos.write(imageBytes);
                    fos.flush();
                }
                AssetFile result = new AssetFile();
                result.src = localFile;
                //result.ext = matcher.group(1);  ---activate when needed---
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        } else if (localOrRemotePath.startsWith("file:/") || !localOrRemotePath.matches("\\w{3,}:\\/.*")) {
            try {
                if (localOrRemotePath.matches("\\w{3,}:\\/.*")) {
                    localOrRemotePath = Pattern.compile("\\w{3,}:\\/(.*)").matcher(localOrRemotePath).group(1);
                }
                //prevents from path exploits on the system
                localOrRemotePath = localOrRemotePath.replace(".." + File.separator, "");
                while (localOrRemotePath.startsWith("/")) {
                    localOrRemotePath = localOrRemotePath.substring(1);
                }
                AssetFile result = new AssetFile();
                result.src = new File(cacheDir, localOrRemotePath);
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {//remote url
            try {
                URL url = new URL(localOrRemotePath);
                String fileName = UUID.randomUUID().toString();
                File downloadedImage = new File(cacheDir, fileName);
                copyURLToFile(url, downloadedImage);
                AssetFile result = new AssetFile();
                result.src = downloadedImage;
                return result;
            } catch (Exception dontCare) {
                dontCare.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Copy url to file.
     *
     * @param source      the source
     * @param destination the destination
     * @throws IOException the io exception
     */
    private static void copyURLToFile(URL source, File destination) throws IOException {
        URLConnection connection = source.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
        InputStream input = connection.getInputStream();
        try {
            FileOutputStream output = openOutputStream(destination);
            try {
                IOUtils.copy(input, output);
            } finally {
                IOUtils.closeQuietly(output);
            }
        } finally {
            IOUtils.closeQuietly(input);
        }
    }


    /**
     * Open output stream file output stream.
     *
     * @param file the file
     * @return the file output stream
     * @throws IOException the io exception
     */
    private static FileOutputStream openOutputStream(File file) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File \'" + file + "\' exists but is a directory");
            }

            if (!file.canWrite()) {
                throw new IOException("File \'" + file + "\' cannot be written to");
            }
        } else {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("File \'" + file + "\' could not be created");
            }
        }

        return new FileOutputStream(file);
    }

}


