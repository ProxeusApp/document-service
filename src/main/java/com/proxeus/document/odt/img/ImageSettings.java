package com.proxeus.document.odt.img;

import com.proxeus.document.AssetFile;
import org.apache.commons.codec.binary.Hex;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ImageSettings contains all necessary information for ImageAdjusterRunnable and more importantly, it calculates a unique ID by the given settings.
 * This way we prevent from starting new threads for the same image container. Which means that in some cases, the same file is taken for multiple containers.
 * It depends on the usage of the variable in the document editor at insert->image->options->name.
 *
 * The name field in the editor must be unique document wide.
 * ImageSettings makes it possible to use the same reference by typing it like:
 *  {{myImageVar}}
 *  {{myImageVar[1]}}
 *  {{myImageVar[2]}}
 *  and so on...
 * {@link #ID()} would calculate in this case the same ID for all.
 * Which means, only one ImageAdjusterRunnable will be started but the image will be used 3 times.
 */
public class ImageSettings {
    private String id;
    protected int containerWidth;
    protected int containerHeight;
    protected double dpi = 300.0; //default
    protected String align = "center";//default
    protected boolean sizeItUpIfSmaller = false;//default
    protected File dst;
    public String varOnly;
    protected String xmlDirPath;
    protected String refFileName;
    public Object localRemoteOrEmbeddedFileObject;
    private final static Pattern imageOptionsRegex = Pattern.compile("(.*)\\[([^\\[\\]]*)\\]");
    private boolean containerDimensionSet = false;
    protected File tmpDir;
    protected Queue<AssetFile> assetFilesToInclude;

    public ImageSettings(String xmlDirPath, String refFileName, String varWithOptions, String containerWidth, String containerHeight, File tmpDir, Queue<AssetFile> assetFilesToInclude) {
        this.xmlDirPath = xmlDirPath;
        this.refFileName = refFileName;
        this.varOnly = varWithOptions;
        Matcher alignMatcher = imageOptionsRegex.matcher(varWithOptions);
        if (alignMatcher.find()) {
            try {
                varOnly = alignMatcher.group(1).trim();
                String options = alignMatcher.group(2);
                String[] opts = options.split(",");
                String[] opt = null;
                for (String op : opts) {
                    opt = op.split(":");
                    if ("align".equals(opt[0].trim())) {
                        align = opt[1].trim();
                    } else if ("size".equals(opt[0].trim())) {
                        sizeItUpIfSmaller = opt[1].trim().matches("fit|yes|true");
                    } else if ("dpi".equals(opt[0].trim())) {
                        try {
                            dpi = Double.parseDouble(opt[1].trim().replace(",", "."));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {/*not important*/}
        }
        if (containerWidth != null && containerHeight != null) {
            try{
                DecimalAndUnit width = strToDecimalAndUnit(containerWidth.toLowerCase());
                DecimalAndUnit height = strToDecimalAndUnit(containerHeight.toLowerCase());
                this.containerWidth = (int) width.getWithDPI(dpi);
                this.containerHeight = (int) height.getWithDPI(dpi);
                if(this.containerWidth > 0 && this.containerHeight > 0){
                    containerDimensionSet = true;
                }
            }catch (Exception e){
                e.printStackTrace();
                //no need to throw it up as it is handled by readyToBeExecuted()
            }
        }
        this.tmpDir = tmpDir;
        this.dst = new File(tmpDir, ID());
    }

    public String ID() {
        if (id == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(containerWidth);
            sb.append(containerHeight);
            sb.append(dpi);
            sb.append(align);
            sb.append(sizeItUpIfSmaller);
            sb.append(varOnly);
            sb.append(refFileName);
            id = stringToHash(sb.toString(), ".png");
        }
        return id;
    }

    private String stringToHash(String plain, String ext) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(plain.getBytes());
            StringBuilder sb = new StringBuilder(36);
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
            }
            sb.append(ext);
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        //fallback
        return Hex.encodeHexString(plain.getBytes()) + ext;
    }

    public boolean readyToBeExecuted() {
        return containerDimensionSet && !dst.exists();
    }

    //created it synchronously to prevent from having multiple threads trying to write to the same destination
    public void touchFile() throws Exception {
        new FileOutputStream(dst).close();
    }

    private final Pattern decimalAndUnitRegex = Pattern.compile("^([0-9\\.]+)(.*)");
    private DecimalAndUnit strToDecimalAndUnit(String str) throws Exception {
        DecimalAndUnit dau = new DecimalAndUnit();
        Matcher imageSub = decimalAndUnitRegex.matcher(str);
        while (imageSub.find()) {
            dau.number = Double.parseDouble(imageSub.group(1));
            dau.unit = imageSub.group(2);
        }
        return dau;
    }
}