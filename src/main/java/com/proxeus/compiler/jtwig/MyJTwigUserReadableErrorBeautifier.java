package com.proxeus.compiler.jtwig;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyJTwigUserReadableErrorBeautifier {
    private Pattern selectBeginningOfErrorPattern = Pattern.compile("\\(Line\\: (\\d+), Column\\: (\\d+)\\) -> ([^\\n]+)");
    private Pattern xmlInsideCodeRegex = Pattern.compile("(<[^>]*>)");
    private Pattern paragraphRegex = Pattern.compile("(<text\\:p[^>]*>)");
    private int margin;
    private Pattern newLine = Pattern.compile("\n");
    private String title = "Compilation error";
    private String titleEndingWithTrackedArea = " in this area:\n";
    private String titleEndingWithUntrackedArea = ":\n";
    private String defaultErrorHelp = "Please review carefully your last changes. Maybe it helps to lookout for missing characters like -> { or } or % . If that doesn't help try to lookout for misspelled wording in the opening expression like -> {%i instead of {%if .";

    public MyJTwigUserReadableErrorBeautifier(){
        this(600);
    }

    public MyJTwigUserReadableErrorBeautifier(int marginOfTheContentToBeVisible){
        margin = marginOfTheContentToBeVisible;
    }

    public String FormatError(InputStream inputStream, Charset charset, Exception e) {
        if(e == null){
            return null;
        }
        String msg = null;
        if(e.getMessage() != null){
            msg = e.getMessage();
        }else if(e.getCause().getMessage() != null){
            msg = e.getCause().getMessage();
        }
        if(msg == null){
            return null;
        }
        try{
            //reset the input stream as we need to read it again for the error
            inputStream.reset();
        }catch (Exception aa){
            //not important
        }

        String content = "";
        try{
            content = IOUtils.toString(inputStream, charset);
        }catch (Exception a1){
            //not important
        }

        if(msg.equals("Invalid template format")){
            return msg+"\n"+defaultErrorHelp;
        }
        Matcher m = selectBeginningOfErrorPattern.matcher(msg);
        if(m.find()){
            int line = Integer.valueOf(m.group(1).trim());
            int col = Integer.valueOf(m.group(2).trim());
            String err = m.group(3);
            err = trackErrorArea(line, col, content, err);
            //in some cases the dirty xml tags are part of the error because of bad code
            //to prevent from an unreadable state of the document, we clean once more
            //bad code example:
            //{{<text:span text:style-name="T59">item}{{loop.index}}
            //   ^
            //Missing or invalid output expression
            return xmlInsideCodeRegex.matcher(paragraphRegex.matcher(err).replaceAll("\n")).replaceAll("");
        }else if(e instanceof org.jtwig.parser.ParseException){
            StringBuilder sb = new StringBuilder(title);
            sb.append(titleEndingWithUntrackedArea);
            sb.append(msg);
            sb.append("\n");
            int target;
            if(msg.length()>0){
                target = msg.length()/2;
                sb.append(targetMarker(target));
            }else{
                target = 0;
                sb.append("^\n");
            }
            sb.append(centerErr(target, defaultErrorHelp));
            sb.append("\n");
            return sb.toString();
        }
        return null;
    }

    public String trackErrorArea(int line, int col, String content, String errorMsg) {
        int left;
        int count = 0;
        int newTarget;
        line--;
        if(line>0){
            Matcher m = newLine.matcher(content);
            while(count < line && m.find()){
                count++;
            }
            if(count>0){
                col = col + m.end();
            }
        }

        if(col > content.length() ){
            col = content.length();
        }

        String linesBefore = "";
        String targetStartHalfLine;
        String targetEndHalfLine;
        String linesAfter;
        int dirtyTagEndingCharIndex;

        targetStartHalfLine = content.substring(0, col);
        targetEndHalfLine = content.substring(col);

        int right = margin;
        if(right > targetEndHalfLine.length()){
            right = targetEndHalfLine.length();
        }
        if((left = col - margin) < 0){
            left = 0;
        }

        targetStartHalfLine = targetStartHalfLine.substring(left, targetStartHalfLine.length());
        if(right>0){
            targetEndHalfLine = targetEndHalfLine.substring(0, right);
        }

        int ni = targetStartHalfLine.lastIndexOf("\n");

        if(ni!=-1){
            linesBefore = linesBefore + targetStartHalfLine.substring(0, ni);
            targetStartHalfLine = targetStartHalfLine.substring(ni);
        }

        if((dirtyTagEndingCharIndex = targetStartHalfLine.indexOf(">"))!=-1){
            targetStartHalfLine = targetStartHalfLine.substring(dirtyTagEndingCharIndex+1, targetStartHalfLine.length());
        }
        String replaceWith = paragraphRegex.matcher(targetStartHalfLine).replaceAll("\n");
        if(targetStartHalfLine.length()!=replaceWith.length()){
            ni = replaceWith.lastIndexOf("\n");
            linesBefore = linesBefore + replaceWith.substring(0, ni);
            targetStartHalfLine = replaceWith.substring(ni);
        }
        targetStartHalfLine = xmlInsideCodeRegex.matcher(targetStartHalfLine).replaceAll("");
        newTarget = targetStartHalfLine.length();
        linesBefore = paragraphRegex.matcher(linesBefore).replaceAll("\n");
        linesBefore = xmlInsideCodeRegex.matcher(linesBefore).replaceAll("");
        targetEndHalfLine = paragraphRegex.matcher(targetEndHalfLine).replaceAll("\n");
        targetEndHalfLine = xmlInsideCodeRegex.matcher(targetEndHalfLine).replaceAll("");
        if((dirtyTagEndingCharIndex = targetEndHalfLine.lastIndexOf("<")) != -1){
            targetEndHalfLine = targetEndHalfLine.substring(0, dirtyTagEndingCharIndex);
        }
        linesBefore = title+titleEndingWithTrackedArea+linesBefore;
        Matcher m = newLine.matcher(targetEndHalfLine);
        if(m.find()){
            linesAfter = targetEndHalfLine;
            targetEndHalfLine = linesAfter.substring(0, m.end());
            linesAfter = linesAfter.substring(m.end());
            return linesBefore + targetStartHalfLine + targetEndHalfLine + targetMarker(newTarget) + centerErr(newTarget, errorMsg) + "\n" + linesAfter + "\n";
        }
        return linesBefore + targetStartHalfLine + targetEndHalfLine + "\n" +targetMarker(newTarget) + centerErr(newTarget, errorMsg) + "\n";
    }

    private String centerErr(int target, String err){
        StringBuilder sb = new StringBuilder();
        if(err == null){
            err = "undefined error";
        }
        target--;
        int l = err.length()/2;
        if(target > l){
            target = target - l;
        }else{
            target = 0;
        }
        if(target>0){
            for(int i = 0; i < target; i++){
                sb.append(" ");
            }
        }
        sb.append(err);
        return sb.toString();
    }

    private String targetMarker(int target) {
        StringBuilder sb = new StringBuilder();
        target--;
        if(target>0){
            for(int i = 0; i < target; i++){
                sb.append(" ");
            }
        }
        sb.append("^\n");
        return sb.toString();
    }
}
