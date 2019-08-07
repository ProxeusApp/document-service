package com.proxeus.office.libre.conn;

import com.sun.star.bridge.UnoUrlResolver;
import com.sun.star.bridge.XUnoUrlResolver;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XDesktop;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.XComponentContext;

import static com.sun.star.uno.UnoRuntime.queryInterface;

/**
 * A bootstrap connector which establishes a connection to an OOo server.
 * <p>
 * Most of the source code in this class has been taken from the Java class
 * "Bootstrap.java" (Revision: 1.15) from the UDK projekt (Uno Software Develop-
 * ment Kit) from OpenOffice.org (http://udk.openoffice.org/). The source code
 * is available for example through a browser based online version control
 * access at http://udk.openoffice.org/source/browse/udk/. The Java class
 * "Bootstrap.java" is there available at
 * http://udk.openoffice.org/source/browse/udk/javaunohelper/com/sun/star/comp/helper/Bootstrap.java?view=markup
 * <p>
 * The idea to develop this BootstrapConnector comes from the blog "Getting
 * started with the OpenOffice.org API part III : starting OpenOffice.org with
 * jars not in the OOo install dir by Wouter van Reeven"
 * (http://technology.amis.nl/blog/?p=1284) and from various posts in the
 * "(Unofficial) OpenOffice.org Forum" at http://www.oooforum.org/ and the
 * "OpenOffice.org Community Forum" at http://user.services.openoffice.org/
 * complaining about "no office executable found!".
 */
public class BootstrapConnector {

    /**
     * The OOo server.
     */
    private OOoServer oooServer;

    /**
     * The connection string which has ben used to establish the connection.
     */
    private String oooConnectionString;

    /**
     * Additional options to add to the oooConnectionString
     */
    protected String additionalOptions = "";

    protected XMultiComponentFactory xmulticomponentfactory;
    protected XComponentContext remoteContext;
    protected Object desktop;
    protected XDesktop xDesktop;
    protected XComponentLoader xCompLoader;

    /**
     * Constructs a bootstrap connector which uses the folder of the OOo
     * installation containing the soffice executable.
     *
     * @param execFoder The folder of the OOo installation containing the soffice executable
     */
    public BootstrapConnector(String execFoder) {
        this.oooServer = new OOoServer(execFoder);
        this.oooConnectionString = null;
    }

    /**
     * Constructs a bootstrap connector which uses the folder of the OOo
     * installation containing the soffice executable.
     *
     * @param execFoder         The folder of the OOo installation containing the soffice executable
     * @param additionalOptions Additional options to add to the oooConnectionString
     */
    public BootstrapConnector(String execFoder, String additionalOptions) {
        this.oooServer = new OOoServer(execFoder);
        this.oooConnectionString = null;
        this.additionalOptions = additionalOptions;
    }

    /**
     * Constructs a bootstrap connector which connects to the specified
     * OOo server.
     *
     * @param oooServer The OOo server
     */
    public BootstrapConnector(OOoServer oooServer) {

        this.oooServer = oooServer;
        this.oooConnectionString = null;
    }

    /**
     * Connects to an OOo server using the specified accept option and
     * connection string and returns a component context for using the
     * connection to the OOo server.
     * <p>
     * The accept option and the connection string should match to get a
     * connection. OOo provides to different types of connections:
     * 1) The socket connection
     * 2) The named pipe connection
     * <p>
     * To create a socket connection a host and port must be provided.
     * For example using the host "localhost" and the port "8100" the
     * accept option and connection string looks like this:
     * - accept option    : -accept=socket,host=localhost,port=8100;urp;
     * - connection string: uno:socket,host=localhost,port=8100;urp;StarOffice.ComponentContext
     * <p>
     * To create a named pipe a pipe name must be provided. For example using
     * the pipe name "oooPipe" the accept option and connection string looks
     * like this:
     * - accept option    : -accept=pipe,name=oooPipe;urp;
     * - connection string: uno:pipe,name=oooPipe;urp;StarOffice.ComponentContext
     *
     * @param oooAcceptOption     The accept option
     * @param oooConnectionString The connection string
     * @return The component context
     */
    public XComponentContext connect(String oooAcceptOption, String oooConnectionString) throws Exception {
        return connect(oooAcceptOption, oooConnectionString, false);
    }

    public XComponentContext connect(String oooAcceptOption, String oooConnectionString, boolean showCmdLineOutput) throws Exception {
        remoteContext = null;
        desktop = null;
        xDesktop = null;
        xCompLoader = null;

        this.oooConnectionString = oooConnectionString;
        for(int c = 0; ; ++c){
            try{
                // get local context
                XComponentContext xLocalContext = getLocalContext();
                oooServer.start(oooAcceptOption, showCmdLineOutput);
                // initial service manager
                XMultiComponentFactory xLocalServiceManager = xLocalContext.getServiceManager();
                if (xLocalServiceManager == null)
                    throw new BootstrapException("no initial service manager!");

                // create a URL resolver
                XUnoUrlResolver xUrlResolver = UnoUrlResolver.create(xLocalContext);
                Thread.sleep(150);
                // wait until office is started
                for (int i = 0; ; ++i) {
                    try {
                        remoteContext = getRemoteContext(xUrlResolver);
                        break;
                    } catch (com.sun.star.connection.NoConnectException ex) {
                        if (i == 50) {
                            throw ex;
                        }
                        Thread.sleep(150);
                    }
                }
            }catch (DisposedException de){
                if(c > 10){
                    throw de;
                }else{
                    //continue trying
                    Thread.sleep(100);
                    continue;
                }
            } catch (Exception e){
                throw e;
            }
            break;
        }
        prepareComps();
        return remoteContext;
    }

    /**
     * Disconnects from an OOo server using the connection string from the
     * previous connect.
     * <p>
     * If there has been no previous connect, the disconnects does nothing.
     * <p>
     * If there has been a previous connect, disconnect tries to terminate
     * the OOo server and kills the OOo server process the connect started.
     */
    public void disconnect() {
        if (oooConnectionString == null)
            return;

        // call office to terminate itself
        try {
            /*
            // get local context
            XComponentContext xLocalContext = getLocalContext();
            //
            // create a URL resolver
            XUnoUrlResolver xUrlResolver = UnoUrlResolver.create(xLocalContext);

             //get remote context
            XComponentContext xRemoteContext = getRemoteContext(xUrlResolver);
            // get desktop to terminate office
            Object desktop = xRemoteContext.getServiceManager().createInstanceWithContext("com.sun.star.frame.Desktop", remoteContext);
            XDesktop xDesktop = queryInterface(XDesktop.class, desktop);
            xDesktop.terminate();
            */
            if (xDesktop != null) {
                xDesktop.terminate();
            }
        } catch (com.sun.star.lang.DisposedException de) {
            //already terminated
        } catch (Exception e) {
            e.printStackTrace();
            // Bad luck, unable to terminate office
        }
        oooServer.kill();
        oooConnectionString = null;
    }

    protected XComponentLoader prepareComps() throws java.lang.Exception {
        if (xCompLoader == null && remoteContext != null) {
            XMultiComponentFactory remoteServiceManager = remoteContext.getServiceManager();
            if (desktop == null) {
                desktop = remoteServiceManager.createInstanceWithContext("com.sun.star.frame.Desktop", remoteContext);
            }
            xCompLoader = queryInterface(XComponentLoader.class, desktop);
            if (xDesktop == null) {
                xDesktop = queryInterface(XDesktop.class, desktop);
            }
        }
        //System.out.println("prepareComps = xCompLoader " + xCompLoader);
        //System.out.println("prepareComps = remoteContext " + remoteContext);
        //System.out.println("prepareComps = desktop " + desktop);
        //System.out.println("prepareComps = xDesktop " + xDesktop);
        return xCompLoader;
    }

    public XComponentLoader getCompLoader() {
        return xCompLoader;
    }

    /**
     * Create default local component context.
     *
     * @return The default local component context
     */
    private XComponentContext getLocalContext() throws BootstrapException, Exception {
        XComponentContext xLocalContext = Bootstrap.createInitialComponentContext(null);
        if (xLocalContext == null) {
            throw new BootstrapException("no local component context!");
        }
        return xLocalContext;
    }

    /**
     * Try to connect to office.
     *
     * @return The remote component context
     */
    private XComponentContext getRemoteContext(XUnoUrlResolver xUrlResolver) throws Exception {
        Object context = xUrlResolver.resolve(oooConnectionString);
        //if(oooConnectionString.contains("forces")){
        //    return getMultiTest(context);
        //}
        XComponentContext xContext = queryInterface(XComponentContext.class, context);
        if (xContext == null) {
            throw new BootstrapException("no component context!");
        }
        return xContext;
    }

    /**
     * Bootstraps a connection to an OOo server in the specified soffice
     * executable folder of the OOo installation using the specified accept
     * option and connection string and returns a component context for using
     * the connection to the OOo server.
     * <p>
     * The accept option and the connection string should match in connection
     * type and pipe name or host and port to get a connection.
     *
     * @param oooExecFolder       The folder of the OOo installation containing the soffice executable
     * @param oooAcceptOption     The accept option
     * @param oooConnectionString The connection string
     * @return The component context
     */
    public static final XComponentContext bootstrap(String oooExecFolder, String oooAcceptOption, String oooConnectionString) throws Exception {
        BootstrapConnector bootstrapConnector = new BootstrapConnector(oooExecFolder);
        return bootstrapConnector.connect(oooAcceptOption, oooConnectionString);
    }
}