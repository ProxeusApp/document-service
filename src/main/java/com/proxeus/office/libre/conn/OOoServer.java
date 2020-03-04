package com.proxeus.office.libre.conn;

import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.lib.util.NativeLibraryLoader;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Starts and stops an OOo server.
 * <p>
 * Most of the source code in this class has been taken from the Java class
 * "Bootstrap.java" (Revision: 1.15) from the UDK projekt (Uno Software Develop-
 * ment Kit) from OpenOffice.org (http://udk.openoffice.org/). The source code
 * is available for example through a browser based online version control
 * access at http://udk.openoffice.org/source/browse/udk/. The Java class
 * "Bootstrap.java" is there available at
 * http://udk.openoffice.org/source/browse/udk/javaunohelper/com/sun/star/comp/helper/Bootstrap.java?view=markup
 */
public class OOoServer {
    /**
     * The OOo server process.
     */
    private Process oooProcess;

    /**
     * The folder of the OOo installation containing the soffice executable.
     */
    private String oooExecFolder;

    /**
     * The options for starting the OOo server.
     */
    private List oooOptions;
    private File tmpFolder;

    /**
     * Constructs an OOo server which uses the folder of the OOo installation
     * containing the soffice executable and a list of default options to start
     * OOo.
     *
     * @param oooExecFolder The folder of the OOo installation containing the soffice executable
     */
    public OOoServer(String oooExecFolder) {
        this.oooProcess = null;
        this.oooExecFolder = oooExecFolder;
        Options ops = getDefaultOOoOptions();
        this.oooOptions = ops.options;
        this.tmpFolder = ops.tmpDir;
    }

    /**
     * Constructs an OOo server which uses the folder of the OOo installation
     * containing the soffice executable and a given list of options to start
     * OOo.
     *
     * @param oooExecFolder The folder of the OOo installation containing the soffice executable
     * @param oooOptions    The list of options
     */
    public OOoServer(String oooExecFolder, List oooOptions) {
        this.oooProcess = null;
        this.oooExecFolder = oooExecFolder;
        this.oooOptions = oooOptions;
    }

    /**
     * Starts an OOo server which uses the specified accept option.
     * <p>
     * The accept option can be used for two different types of connections:
     * 1) The socket connection
     * 2) The named pipe connection
     * <p>
     * To create a socket connection a host and port must be provided.
     * For example using the host "localhost" and the port "8100" the
     * accept option looks like this:
     * - accept option    : -accept=socket,host=localhost,port=8100;urp;
     * <p>
     * To create a named pipe a pipe name must be provided. For example using
     * the pipe name "oooPipe" the accept option looks like this:
     * - accept option    : -accept=pipe,name=oooPipe;urp;
     *
     * @param oooAcceptOption The accept option
     */
    public void start(String oooAcceptOption, boolean showCmdLineOutput) throws BootstrapException, IOException, MalformedURLException {

        // find office executable relative to this class's class loader
        String sOffice = System.getProperty("os.name").startsWith("Windows") ? "soffice.exe" : "soffice";

        URL[] oooExecFolderURL = new URL[]{new File(oooExecFolder).toURI().toURL()};
        URLClassLoader loader = new URLClassLoader(oooExecFolderURL);
        File fOffice = NativeLibraryLoader.getResource(loader, sOffice);
        if (fOffice == null)
            throw new BootstrapException("no office executable found!");

        // create call with arguments
        int arguments = (oooOptions != null) ? oooOptions.size() + 1 : 1;
        if (oooAcceptOption != null)
            arguments++;

        String[] oooCommand = new String[arguments];
        oooCommand[0] = fOffice.getPath();

        for (int i = 0; i < oooOptions.size(); i++) {
            oooCommand[i + 1] = (String) oooOptions.get(i);
        }

        if (oooAcceptOption != null)
            oooCommand[arguments - 1] = oooAcceptOption;

        // start office process
        oooProcess = Runtime.getRuntime().exec(oooCommand);
        if (showCmdLineOutput) {
            pipe(oooProcess.getInputStream(), System.out, "Output> ");
            pipe(oooProcess.getErrorStream(), System.err, "Error> ");
        }
    }

    /**
     * Kills the OOo server process from the previous start.
     * <p>
     * If there has been no previous start of the OOo server, the kill does
     * nothing.
     * <p>
     * If there has been a previous start, kill destroys the process.
     */
    public void kill() {
        if (oooProcess != null) {
            oooProcess.destroyForcibly();
            oooProcess = null;
            try {
                FileUtils.deleteDirectory(tmpFolder);
            } catch (Exception e) {
                //not important
            }
        }
    }

    private static void pipe(final InputStream in, final PrintStream out, final String prefix) {
        new Thread("Pipe: " + prefix) {
            @Override
            public void run() {
                BufferedReader r = new BufferedReader(new InputStreamReader(in));
                try {
                    for (; ; ) {
                        String s = r.readLine();
                        if (s == null) {
                            break;
                        }
                        out.println(prefix + s);
                    }
                } catch (IOException e) {
                    System.out.println("exception with "+prefix+" "+e.getMessage());
                    e.printStackTrace();
                }
            }
        }.start();
    }


    /**
     * Returns the list of default options.
     *
     * @return The list of default options
     */
    public static Options getDefaultOOoOptions() {
        Options o = new Options();
        o.options = new ArrayList<>(6);

        o.options.add("--nologo");
        o.options.add("--nodefault");
        o.options.add("--norestore");
        o.options.add("--nocrashreport");
        o.options.add("--nolockcheck");

        String OOUserIntallationTempFolder = "/loui_" + UUID.randomUUID().toString();
        o.tmpDir = new File(System.getProperty("java.io.tmpdir"), OOUserIntallationTempFolder);
        o.options.add("-env:UserInstallation=file:///" + System.getProperty("java.io.tmpdir") + OOUserIntallationTempFolder);
        return o;
    }
}