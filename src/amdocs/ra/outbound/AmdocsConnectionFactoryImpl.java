package amdocs.ra.outbound;

import amdocs.ra.client.AmdocsConnection;
import amdocs.ra.client.AmdocsConnectionFactory;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.Referenceable;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ManagedConnectionFactory;
/*
 * AmdocsConnectionFactoryImpl.java
 *
 * Created on 2006. május 9., 12:34
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 * A reference to an instance of this class can be located in the NamingService
 * of the server that runs the connector. Use this reference to create connections
 * that needed to attach to the Amdocs EJB API.
 * <p>
 * Follow these steps during connection creation to Amdocs EJB API:<br>
 * 1. lookup a reference to an instance of this class in the runner
 *   server's NamingService under "eis" context.<br>
 * 2. call createConnection(ConnectionSpecImpl) on the object reference
 *   got in previous step.<br>
 *
 * @author attila.rezner
 */
public class AmdocsConnectionFactoryImpl
implements AmdocsConnectionFactory, Referenceable, Serializable {
    /** @see java.util.logging.Logger */
    private static Logger logger = 
        Logger.getLogger("javax.enterprise.resource.resourceadapter");
    
    /**
     * {@link ManagedConnectionFactoryImpl ManagedConnectionFactoryImpl} that
     * created this AmdocsConnectionFactoryImpl
     */
    private ManagedConnectionFactoryImpl manConFactImpl;
    /**
     * in a managed environment ConnectionManager is provided by the AppServer,
     * in a non-managed environment the connector implements it.
     * <p>
     * Here is the implementation of the {@link ConnectionManagerImpl ConnectionManagerImpl}
     * by the Amdocs connector.
     */
    private ConnectionManager conMan;
    /** */
    private Reference reference;
    /** */
    private transient PrintWriter out;
    /** creation date of this AmdocsConnectionFactoryImpl */
    private Date creationDate;

    /**
     * Currently not used.
     * <p>
     * Creates a new instance of AmdocsConnectionFactoryImpl
     */
    public AmdocsConnectionFactoryImpl() {
    }

    /**
     * Created from ManagedConnectionFactory's createConnectionFactory method
     * called by container after ManagedConnectionFactoryImpl.CTOR.
     *
     * @param   mcf         {@link ManagedConnectionFactoryImpl ManagedConnectionFactory}
     *                      got from container.
     * @param   cm          {@link ConnectionManagerImpl ConnectionManager}
     *                      got from container.
     */
    public AmdocsConnectionFactoryImpl(
        ManagedConnectionFactory mcf, ConnectionManager cm) {

        manConFactImpl = (ManagedConnectionFactoryImpl) mcf;
        // log
        logger.log(Level.FINEST, "AmdocsConnectionFactoryImpl CTOR(" +
            "ManagedConnectionFactoryImpl " +manConFactImpl.toString() +
            ", ConnectionManagerImpl " +(cm == null ? "from adapter" : "from server") +")");
        
        if (cm == null) {
            conMan = new ConnectionManagerImpl();
        }
        else {
            conMan = cm;
        }
        creationDate = new Date();
    }

    // <editor-fold defaultstate="collapsed" desc=" inherited from AmdocsConnectionFactory ">
    /**
     * Currently not used.
     *
     * @throws      javax.resource.ResourceException on any error.
     * @return      AmdocsConnectionImpl which implements AmdocsConnection IF.
     */
    public AmdocsConnection createConnection() throws ResourceException {
        /** */
        synchronized (this) {
            logger.log(Level.FINEST, "AmdocsConnectionFactoryImpl" +
                " createConnection -without EntityName " +toString());
            
            throw new ResourceException(
                "Cannot create AmdocsConnection without EntityName!");
            }
    }

    /**
     * Creates a connection handle to a specific Amdocs EJB.
     *
     * @param   amdocsEjbJNDIname   JNDI name of Amdocs EJB intended to use.
     * @param   homeClassName       home class name of Amdocs EJB in JNDI.
     * @param   remoteClassName     remote class name of Amdocs EJB in JNDI.
     * @throws                      javax.resource.ResourceException on any error.
     * @return                      {@link amdocs.ra.outbound.AmdocsConnectionImpl
     *                              AmdocsConnection}.
     */
    public AmdocsConnection createConnection(
        String amdocsEjbJNDIname, String homeClassName, String remoteClassName)
    throws ResourceException {

        /**
         * multiple requests may need new connections simultaneously so new
         * connection creation must be thread-safe
         */
        synchronized (this) {
            logger.log(Level.INFO, "AmdocsConnectionFactoryImpl createConnection" +
                toString() +"[ConnectionSpecImpl " +amdocsEjbJNDIname +"]");
            
            ConnectionRequestInfoImpl connReqInfo = new ConnectionRequestInfoImpl(
                amdocsEjbJNDIname, homeClassName,remoteClassName);

            AmdocsConnectionImpl amdocsConnectionImpl = (AmdocsConnectionImpl)
                /**
                 * in conMan.allocateConnection:
                 * 1. managedConnectionFactory.createManagedConnection
                 * 2. managedConnection.getConnection
                 */
                conMan.allocateConnection(manConFactImpl, connReqInfo);
            return (AmdocsConnection)amdocsConnectionImpl;
        }
    }

    /**
     * Call this method only in non-managed environment to get reference to
     * ConnecionManager. In this case adapter directly controls pool management
     * providing a class that implements ConnecionManager IF.
     *
     * @return      an object instance that implements ConnectionManager IF.
     */
    public ConnectionManager getConnectionManager() {
        logger.log(Level.FINEST, "AmdocsConnectionFactoryImpl getConnectionManager");
        
        return conMan;
    }
    // </editor-fold>

    /**
     * Sets the logger - log will be printed in the Server log.
     *
     * @throws      javax.resource.ResourceException on any error.
     * @param       out got from container (reference to Server's out).
     */
    public void setLogWriter(PrintWriter out) throws ResourceException {
        this.out = out;
    }

    /**
     * Provides reference to Server's out object.
     *
     * @return      reference to Server's out object.
     * @throws      javax.resource.ResourceException on any error.
     */
    public PrintWriter getLogWriter() throws ResourceException {
        return out;
    }

    // <editor-fold defaultstate="collapsed" desc=" inherited from Referenceable ">
    /**
     * Inherited from Referenceable. Implementing this IF makes possible to create
     * a reference to an instance of this class in NamingService.
     *
     * @param       reference got from container.
     */
    public void setReference(Reference reference) {
        logger.log(Level.FINEST, "AmdocsConnectionFactoryImpl setReference");
        
        this.reference = reference;
    }

    /**
     * Inherited from Referenceable. Implementing this IF makes possible to create
     * a reference to an instance of this class in NamingService.
     *
     * @return      reference.
     *
     * @throws      javax.naming.NamingException on any error.
     */
    public Reference getReference() throws NamingException {
        logger.log(Level.FINEST, "AmdocsConnectionFactoryImpl getReference");
        
        return reference;
    }
    // </editor-fold>

    /**
     * Provides information about data stored in this instance.
     *
     * @return      printable string containing data stored in fields.
     */
    @Override
    public String toString() {
        return "[creation date " +creationDate +
            ", creator ManagedConnectionFactory " +manConFactImpl.toString() +"]";
    }

}
