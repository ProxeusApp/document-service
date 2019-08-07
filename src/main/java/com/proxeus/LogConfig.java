package com.proxeus;

public class LogConfig {
    public String level = "INFO";
    /**
     * OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE, ALL
     **/
    public String level_console = "DEBUG";
    /**
     * OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE, ALL
     **/
    public String maxFileSize = "5MB";
    public int maxBackupIndex = 5;
    public String pattern = "%d{dd.MM.yyyy HH:mm:ss} %-5p %c{1}:%L - %m%n";
    public String filePath = "./logs/document-service.log";
}