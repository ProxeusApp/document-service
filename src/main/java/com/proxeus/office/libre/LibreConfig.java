package com.proxeus.office.libre;

/**
 * LibreConfig to initialize LibreOfficeAssistant.
 * These are the defaults that are set here, they are going to be overwritten by the config, if defined.
 */
public class LibreConfig {
    /**
     * "/opt/libreoffice5.4/program" "C:/Program Files/LibreOffice 5/program" "/usr/lib/libreoffice/program"
     * "/Applications/LibreOffice.app/Contents/MacOS/soffice"
     **/
    public String librepath = "/usr/lib/libreoffice/program";
    /**
     * min executables ready to be ready. An executable is mainly needed to convert to PDF. It is recommended to use one exe for a request at the time.
     **/
    public int min = 8;
    /**
     * max capacity of executable running. The next request will be on hold until one is freed or until request timeout.
     **/
    public int max = 40;

    /** highLoad defines the percentage of executables in use, when it is reached prepare new ones to be ready for high availability and fast response.**/
    public int highLoad = 60;
}
