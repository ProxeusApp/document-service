package com.proxeus.document.odt;

import org.apache.commons.text.StringEscapeUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class helps to make complicated errors inside an ODT document visible. This way it is easier to track the mistakes and fix them.
 * TODO improve the performance and memory usage by avoiding string modifications as much as possible
 */
public class ODTReadableErrorHandler {
    private String errFrame;
    private String errStyle;
    private String stylesEndingTag = "</office:automatic-styles>";
    private Pattern errFrameStartRegex = Pattern.compile("<draw:frame [^>]*draw:style-name=\"ERRgr1\"[^>]*>");
    private Pattern errFrameEndRegex = Pattern.compile("<\\/draw\\:frame>");
    private Pattern xmlVersionRegex = Pattern.compile("<\\?[^>]+version=\"([^\\\"]+)\"[^>]+>");

    public ODTReadableErrorHandler(){
        errStyle = odtErrorStyle();
        errFrame = odtErrorFrame();
    }

    public String setErrorMessage(String contentXml, String error){
        error = escapeXML(contentXml, error);

        //add error styles if not already added
        //they can be added if the template result was requested as an ODT and used again
        int indexOfStylesEnding = contentXml.indexOf(stylesEndingTag);
        if(indexOfStylesEnding!=-1){
            //set styles only if global styles exists
            String toCheckIfAlreadyIncluded = contentXml.substring(0, indexOfStylesEnding);
            if(!toCheckIfAlreadyIncluded.contains("ERR_MID_MSG")){
                contentXml = contentXml.replace(stylesEndingTag, errStyle);
            }
        }

        contentXml = removeErrorFrameIfAlreadyExists(contentXml);

        //add new error frame
        String errFrameWithError = errFrame.replaceFirst("___eRRoR___", createErrorMessage(error));
        if (contentXml.contains("</text:sequence-decls>")) {
            contentXml = contentXml.replace("</text:sequence-decls>", "</text:sequence-decls>" + errFrameWithError);
        } else if (contentXml.contains("<office:text>")) {
            contentXml = contentXml.replace("<office:text>", "<office:text>" + errFrameWithError);
        }
        return contentXml;
    }

    private String removeErrorFrameIfAlreadyExists(String contentXml){
        Matcher errFrameAlreadyExistsMatcher = errFrameStartRegex.matcher(contentXml);
        if(errFrameAlreadyExistsMatcher.find()){
            int sStart = errFrameAlreadyExistsMatcher.start();
            int sEnd = errFrameAlreadyExistsMatcher.end();
            Matcher errFrameAlreadyExistsEndMatcher = errFrameEndRegex.matcher(contentXml);
            if(errFrameAlreadyExistsEndMatcher.find(sEnd)){
                int eEnd = errFrameAlreadyExistsEndMatcher.end();
                contentXml = contentXml.substring(0, sStart) + contentXml.substring(eEnd);
            }
        }
        return contentXml;
    }

    private String escapeXML(String contentXml, String error){
        /**
         * XML escapes are not working correctly for ODT
        double xmlVersion = readXmlVersion(contentXml);
        if(xmlVersion>1){
            error = StringEscapeUtils.escapeXml11(error);
        }else{
            error = StringEscapeUtils.escapeXml10(error);
        }
         **/
        error = StringEscapeUtils.escapeXml10(error);
        return error;
    }

    private double readXmlVersion(String contentXml){
        Matcher xmlVersion = xmlVersionRegex.matcher(contentXml);
        if(xmlVersion.find()){
            try{
                return Double.valueOf(xmlVersion.group(1));
            }catch (Exception e){}
        }
        return 1.0;
    }

    private String createErrorMessage(String error){
        if (error == null) {
            error = "Unknown error";
        }
        String[] errLines = error.split("\n");
        StringBuilder sbErr = new StringBuilder();
        for(int i = 0; i < 2; i++){
            sbErr.append("<text:p text:style-name=\"PERR1\">");
            sbErr.append("<text:span text:style-name=\"TERR1\">");
            sbErr.append(errLines[0]);
            errLines[0] = "";
            sbErr.append("</text:span></text:p>");
        }

        int targetLine = -1;
        int charCount = 0;
        int margin = 10;
        int left;
        int right;
        String txtStyle;
        String pStyle;
        for(int i = 1; i < errLines.length; i++){
            if(errLines[i].trim().equals("^")){
                targetLine = i-1;
                charCount = errLines[i].indexOf("^");
                break;
            }
        }

        for(int i = 1; i < errLines.length; i++){
            txtStyle = "ERRT1026";
            pStyle = "PERR1";
            errLines[i] = errLines[i].trim();
            if(i == targetLine){
                if((left=charCount-margin)<0){
                    left = 0;
                }
                if((right=charCount+margin)>errLines[i].length()){
                    right = errLines[i].length();
                }
                sbErr.append("<text:p text:style-name=\"PERR1\">");
                sbErr.append("<text:span text:style-name=\"ERRT1026\">");
                sbErr.append(errLines[i].substring(0, left));
                sbErr.append("</text:span>");

                sbErr.append("<text:span text:style-name=\"ERR_TARGET\">");
                sbErr.append(errLines[i].substring(left, right));
                sbErr.append("</text:span>");

                sbErr.append("<text:span text:style-name=\"ERRT1026\">");
                sbErr.append(errLines[i].substring(right));
                sbErr.append("</text:span></text:p>");
                continue;
            }else if(i == targetLine+1){
                continue;
            }else if(i == targetLine+2){
                sbErr.append("<text:p text:style-name=\"PERR1\"><text:span text:style-name=\"ERRT1026\"></text:span></text:p>");
                txtStyle = "ERR_MID_MSG";
                pStyle = "PERR_MID";
            }
            sbErr.append("<text:p text:style-name=\"");
            sbErr.append(pStyle);
            sbErr.append("\">");
            sbErr.append("<text:span text:style-name=\"");
            sbErr.append(txtStyle);
            sbErr.append("\">");
            sbErr.append(errLines[i]);
            sbErr.append("</text:span></text:p>");
            if(i == targetLine+2){
                sbErr.append("<text:p text:style-name=\"PERR1\"><text:span text:style-name=\"ERRT1026\"></text:span></text:p>");
            }
        }
        return sbErr.toString();
    }

    private String odtErrorFrame() {
        return "<draw:frame text:anchor-type=\"page\" text:anchor-page-number=\"1\" " +
                "       draw:z-index=\"12\" draw:name=\"Shape1\" draw:style-name=\"ERRgr1\" " +
                "       draw:text-style-name=\"ERRP384\" svg:width=\"8.2705in\" " +
                "       svg:height=\"5.1685in\" svg:x=\"0in\" svg:y=\"1.0598in\">" +
                "   <draw:text-box draw:corner-radius=\"0.2201in\">" +
                "       ___eRRoR___" +
                "   </draw:text-box>" +
                "</draw:frame>\n";
    }

    private String odtErrorStyle() {
        String titleFontSize = "16pt";
        String fontSize = "10pt";
        String midMsgFontSize = "12pt";
        return "<style:style style:name=\"PERR1\" style:family=\"paragraph\" style:parent-style-name=\"Frame_20_contents\">\n" +
                "<style:paragraph-properties fo:text-align=\"left\" style:justify-single-word=\"false\"/>\n" +
                "<style:text-properties fo:font-size=\""+fontSize+"\" style:font-size-asian=\""+fontSize+"\" style:font-size-complex=\""+fontSize+"\" fo:color=\"#faa61a\" style:font-name=\"FreeMono\" officeooo:rsid=\"000491a9\" officeooo:paragraph-rsid=\"000491a9\"/>\n" +
                "</style:style>\n" +
                "<style:style style:name=\"PERR_MID\" style:family=\"paragraph\" style:parent-style-name=\"Frame_20_contents\">\n" +
                "<style:paragraph-properties fo:text-align=\"center\" style:justify-single-word=\"false\"/>\n" +
                "<style:text-properties fo:font-size=\""+midMsgFontSize+"\" style:font-size-asian=\""+midMsgFontSize+"\" style:font-size-complex=\""+midMsgFontSize+"\" fo:color=\"#faa61a\" style:font-name=\"FreeMono\" officeooo:rsid=\"000491a9\" officeooo:paragraph-rsid=\"000491a9\"/>\n" +
                "</style:style>\n" +
                "<style:style style:name=\"ERR_TARGET\" style:family=\"text\"><style:text-properties fo:color=\"#faa61a\" style:font-name=\"FreeMono\" fo:font-size=\""+fontSize+"\" style:text-underline-style=\"wave\" style:text-underline-width=\"bold\" style:text-underline-color=\"#ff4500\" fo:font-weight=\"normal\" style:font-size-asian=\""+fontSize+"\" style:font-weight-asian=\"normal\" style:font-size-complex=\""+fontSize+"\" style:font-weight-complex=\"normal\"/></style:style>" +
                "<style:style style:name=\"ERR_MID_MSG\" style:family=\"text\"><style:text-properties fo:color=\"#faa61a\" style:font-name=\"FreeMono\" fo:font-size=\""+midMsgFontSize+"\" fo:font-weight=\"bold\" style:font-size-asian=\""+midMsgFontSize+"\" style:font-weight-asian=\"bold\" style:font-size-complex=\""+midMsgFontSize+"\" style:font-weight-complex=\"bold\"/></style:style>" +
                "<style:style style:name=\"ERRP383\" style:family=\"paragraph\">" +
                "   <style:text-properties style:font-name=\"FreeMono\" fo:color=\"#ce181e\"/></style:style>\n" +
                "<style:style style:name=\"ERRT1026\" style:family=\"text\"><style:text-properties style:font-name=\"FreeMono\" fo:color=\"#faa61a\"/></style:style>" +
                "<style:style style:name=\"TERR1\" style:family=\"text\"><style:text-properties style:font-name=\"FreeMono\" fo:color=\"#faa61a\" fo:font-size=\""+titleFontSize+"\" fo:font-weight=\"bold\" style:font-size-asian=\""+titleFontSize+"\" style:font-weight-asian=\"bold\" style:font-size-complex=\""+titleFontSize+"\" style:font-weight-complex=\"bold\"/></style:style>" +
                "<style:style style:name=\"ERRgr1\" style:family=\"graphic\"><style:graphic-properties draw:stroke=\"none\" svg:stroke-color=\"#000000\" draw:fill=\"solid\" draw:fill-color=\"#000000\" draw:opacity=\"88%\" " +
                "   fo:padding-top=\"0.4in\" fo:padding-bottom=\"0.4in\" fo:padding-left=\"0.4in\" fo:padding-right=\"0.4in\" draw:shadow-offset-x=\"0.1201in\" draw:shadow-offset-y=\"0.1201in\" draw:shadow-opacity=\"88%\" style:run-through=\"foreground\" style:wrap=\"run-through\" style:number-wrapped-paragraphs=\"no-limit\" style:vertical-pos=\"from-top\" style:vertical-rel=\"page\" style:horizontal-pos=\"from-left\" style:horizontal-rel=\"page\"/></style:style>" +
                "<style:style style:name=\"ERRP384\" style:family=\"paragraph\"><loext:graphic-properties draw:fill=\"solid\" draw:fill-color=\"#000000\" draw:opacity=\"88%\"/><style:text-properties style:font-name=\"FreeMono\" fo:color=\"#ce181e\"/></style:style></office:automatic-styles>";
    }
}
