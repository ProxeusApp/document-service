package com.proxeus.office.libre.conn;

import com.sun.star.uno.XComponentContext;

import java.util.UUID;

/**
 * A bootstrap connector which uses a named pipe to connect to an OOo server.
 * 
 * Very helpful in getting the named pipe connection working has been the posts
 * of mnasato in the thread "Correct FilterName to open RTF from bytestream?" at
 * http://www.oooforum.org/forum/viewtopic.phtml?t=40263&highlight=named+pipe.
 */
public class BootstrapPipeConnector extends BootstrapConnector {

    /**
     * Constructs a bootstrap pipe connector which uses the specified folder of
     * the OOo installation containing the soffice executable.
     * 
     * @param   oooExecFolder   The folder of the OOo installation containing the soffice executable
     */
    public BootstrapPipeConnector(String oooExecFolder) {
        super(oooExecFolder);
    }

    /**
     * Constructs a bootstrap pipe connector which uses the specified folder of
     * the OOo installation containing the soffice executable.
     *
     * @param   oooExecFolder   The folder of the OOo installation containing the soffice executable
     * @param   additionalOptions   Additional options to add to the oooConnectionString
     */
    public BootstrapPipeConnector(String oooExecFolder, String additionalOptions)
    {
        super(oooExecFolder, additionalOptions);
    }

    /**
     * Constructs a bootstrap pipe connector which connects to the specified
     * OOo server.
     * 
     * @param   oooServer   The OOo server
     */
    public BootstrapPipeConnector(OOoServer oooServer) {
        super(oooServer);
    }

    /**
     * Connects to an OOo server using a random pipe name and returns a
     * component context for using the connection to the OOo server.
     * 
     * @return             The component context
     */
    public XComponentContext connect() throws Exception {
        // create random pipe name
        String sPipeName = "uno_" + UUID.randomUUID().toString();
        return connect(sPipeName);
    }

    /**
     * Connects to an OOo server using the specified pipe name and returns a
     * component context for using the connection to the OOo server.
     * 
     * @param   pipeName   The pipe name
     * @return             The component context
     */
    public XComponentContext connect(String pipeName) throws Exception {
        // accept option
        String oooAcceptOption = "--accept=pipe,name=" + pipeName + ";urp;" + additionalOptions;
        // connection string
        String unoConnectString = "uno:pipe,name=" + pipeName + ";urp;StarOffice.ComponentContext";
        return connect(oooAcceptOption, unoConnectString);
    }

    /**
     * Bootstraps a connection to an OOo server in the specified soffice
     * executable folder of the OOo installation using a random pipe name and
     * returns a component context for using the connection to the OOo server.
     * 
     * @param   oooExecFolder      The folder of the OOo installation containing the soffice executable
     * @return                     The component context
     */
    public static final XComponentContext bootstrap(String oooExecFolder) throws Exception {
        BootstrapPipeConnector bootstrapPipeConnector = new BootstrapPipeConnector(oooExecFolder);
        return bootstrapPipeConnector.connect();
    }

    /**
     * Bootstraps a connection to an OOo server in the specified soffice
     * executable folder of the OOo installation using the specified pipe name
     * and returns a component context for using the connection to OOo server.
     * 
     * @param   oooExecFolder      The folder of the OOo installation containing the soffice executable
     * @param   pipeName           The pipe name
     * @return                     The component context
     */
    public static final XComponentContext bootstrap(String oooExecFolder, String pipeName) throws Exception {
        BootstrapPipeConnector bootstrapPipeConnector = new BootstrapPipeConnector(oooExecFolder);
        return bootstrapPipeConnector.connect(pipeName);
    }
}