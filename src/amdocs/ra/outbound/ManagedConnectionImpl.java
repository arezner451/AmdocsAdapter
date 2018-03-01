package amdocs.ra.outbound;

import amdocs.ra.client.AmdocsConnection;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
/*
 * ManagedConnectionImpl.java
 *
 * Created on 2006. április 4., 13:46
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 * ManagedConnection represents a physical connection to EIS. There is a pool of
 * ManagedConnections on the AppServer.
 *
 * @author attila.rezner
 */
public class ManagedConnectionImpl implements ManagedConnection {
    /** @see java.util.logging.Logger */
    private static Logger logger = 
        Logger.getLogger("javax.enterprise.resource.resourceadapter");
    /**
     * {@link ManagedConnectionFactoryImpl ManagedConnectionFactoryImpl} which
     * created this ManagedConnectionImpl
     */
    private ManagedConnectionFactoryImpl manConFactImpl;
    
    /** {@link ConnectionRequestInfoImpl conReqInfImpl} */
    private ConnectionRequestInfoImpl conReqInfImpl;
    
    /**  */
    private PrintWriter printWriter;
    
    /** set of Connection Handles to this ManagedConnectionImpl */
    private Set<AmdocsConnectionImpl> connectionHandles;
    
    /**
     * registered connectionEventListeners -by AppServer- to this
     * ManagedConnectionImpl
     */
    private Set<ConnectionEventListener> connectionEventListeners;
    
    /**
     * {@link AmdocsGateway amdocsGateway} member variable is created in CTOR... if null
     * then this ManagedConnectionImpl has no physical connection to EIS - can
     * be removed from pool.
     */
    protected AmdocsGateway amdocsGateway;
    
    /** creation date of this ManagedConnectionImpl */
    private Date creationDate, 
    /** date of last invocation of getAmdocsRef of this ManagedConnectionImpl */            
                 lastUsageDate;
    
    /**
     * Creates a new instance of ManagedConnectionImpl, so creates a new physical
     * connection to EIS.
     */
    public ManagedConnectionImpl() {
    }

    /**
     * NamingException can raised by new Amdocs() when no entity found in JNDI with the
     * supplied name.
     * <p>
     * Called by container after ManagedConnectionFactoryImpl equals.
     *
     * @param   mcf             {@link ManagedConnectionFactoryImpl ManagedConnectionFactoryImpl}
     *                          that creates this ManagedConnectionImpl.
     * @param   subject         got from container.
     * @param   cxRequestInfo   {@link ConnectionRequestInfoImpl ConnectionRequestInfo}.
     */
    public ManagedConnectionImpl(
        ManagedConnectionFactory mcf, Subject subject, ConnectionRequestInfo cxRequestInfo) {

        creationDate = new Date();

        manConFactImpl = (ManagedConnectionFactoryImpl)mcf;
        conReqInfImpl = (ConnectionRequestInfoImpl)cxRequestInfo;

        logger.log(Level.INFO, "ManagedConnectionImpl CTOR(" +
            "ManagedConnectionFactoryImpl " +getManConFactImpl().toString() +
            ", ConnectionRequestInfoImpl " +conReqInfImpl.toString() +")");
    
        // <editor-fold defaultstate="collapsed" desc=" security handling ">
        PasswordCredential passwordCredential = new PasswordCredential(
            getManConFactImpl().getUserName(), getManConFactImpl().getPassword().toCharArray());

        passwordCredential.setManagedConnectionFactory(getManConFactImpl());
        // </editor-fold>
        // to contain connection handles of this ManagedConnectionImpl
        connectionHandles = new HashSet<AmdocsConnectionImpl>();
        // to contain references to connectionEventListeners provided by AppServer
        connectionEventListeners = new HashSet<ConnectionEventListener>();
        // Open the physical connection: connect to amdocs...
        ConnectionSpecImpl conSpecImpl = new ConnectionSpecImpl();
        conSpecImpl.setEntityName(conReqInfImpl.getEntityName());
        conSpecImpl.setHomeClassName(conReqInfImpl.getHomeClassName());
        conSpecImpl.setRemoteClassName(conReqInfImpl.getRemoteClassName());
        //
        amdocsGateway = new AmdocsGateway(this, conSpecImpl);
    }

    // <editor-fold defaultstate="collapsed" desc=" inherited from ManagedConnection ">
    /**
     * gets a connection handle to the physical EIS connection.
     * <p>
     * Called from ConnectionManager.allocateConnection.
     *
     * @param   subject         got from container.
     * @param   cxRequestInfo   {@link ConnectionRequestInfoImpl ConnectionRequestInfo}.
     * @return  a newly created {@link AmdocsConnectionImpl AmdocsConnectionImpl}
     *          which is a handle to this ManagedConnectionImpl.
     * @throws  javax.resource.ResourceException generic exception if operation
     *          fails due to an error condition.
     */
    public Object getConnection(Subject subject, ConnectionRequestInfo cxRequestInfo)
    throws ResourceException {

        ConnectionRequestInfoImpl conReqInfImpl =
            (ConnectionRequestInfoImpl)cxRequestInfo;

        logger.log(Level.INFO, "ManagedConnectionImpl getConnection " +toString() +
            ", [ConnectionRequestInfoImpl " +conReqInfImpl.toString() +"]");
    
        // <editor-fold defaultstate="collapsed" desc=" security handling ">
        PasswordCredential passwordCredential = new PasswordCredential(
            getManConFactImpl().getUserName(), getManConFactImpl().getPassword().toCharArray());

        passwordCredential.setManagedConnectionFactory(getManConFactImpl());
        // </editor-fold>
        AmdocsConnectionImpl amdocsConnectionImpl = new AmdocsConnectionImpl(this);
        addAmdocsConnection(amdocsConnectionImpl);

        return (AmdocsConnection)amdocsConnectionImpl;
    }

    /**
     * AppServer calls this method when receive connection error event
     * notification that signals a fatal error on the physical connection.
     * <p>
     * ManagedConnectionImpl instance has to close the connection handles and
     * release all system resources held by this instance.
     * <p>
     * Called from {@link amdocs.ra.ResourceAdapterImpl#stop ResourceAdapterImpl.stop}.
     *
     * @throws  javax.resource.ResourceException generic exception if operation
     *          fails due to an error condition.
     */
    public void destroy() throws ResourceException {
        logger.log(Level.FINEST, "ManagedConnectionImpl destroy " +toString());
        /**
         * decrease this ManagedConnectionFactoryImpl's manConnCount and
         * remove this ManagedConnectionImpl instance from factory's manConns.
         */
        Set factorysManagedConns = manConFactImpl.getManagedConnsOfFactory();
        factorysManagedConns.remove(this);
        /**  */
        Iterator<AmdocsConnectionImpl> connectionHandlesIter =
            connectionHandles.iterator();

        while (connectionHandlesIter.hasNext()) {
            AmdocsConnectionImpl amdocsConnectionImpl = connectionHandlesIter.next();
            /**
             * sets amdocsConnectionImpl's ManagedConnectionImpl to null
             * handle removes oneself from this instance's handle list
             */
            amdocsConnectionImpl.invalidate();
        }
        /** close the physical connection, free resources: set to null so gc cleans up */
        if (amdocsGateway != null) {
            /** remove remoteBeanIF */
            amdocsGateway.releaseAPI("from ManagedConnectionImpl destroy ");
            amdocsGateway = null;
        }
    }

    /**
     * Called by the AppServer when the application component instance that has
     * the corresponding handle terminates.
     * <p>
     * After cleanup AppServer puts back this ManagedConnection instance into
     * the pool to server future allocation requests.
     *
     * @throws      javax.resource.ResourceException generic exception if
     *              operation fails due to an error condition.
     */
    public void cleanup() throws ResourceException {
        logger.log(Level.FINEST, "ManagedConnectionImpl cleanup " +toString());

        Iterator<AmdocsConnectionImpl> connectionHandlesIter =
            connectionHandles.iterator();

        while (connectionHandlesIter.hasNext()) {
            AmdocsConnectionImpl amdocsConnectionImpl = connectionHandlesIter.next();

            /**
             * sets amdocsConnectionImpl's ManagedConnectionImpl to null
             * handle removes oneself from ManagedConnectionImpl's handle list
             */
            amdocsConnectionImpl.invalidate();
        }
    }

    /**
     * Used by the container to change the association of an application-level
     * connection handle with a ManagedConneciton instance.
     * The container should find the right ManagedConnection instance.
     *
     * @param       connection      {@link AmdocsConnectionImpl AmdocsConnectionImpl}
     *                              that should be add to this managedConnectionImpl's
     *                              handle set.
     * @throws      javax.resource.ResourceException generic exception if
     *              operation fails due to an error condition.
     */
    public void associateConnection(Object connection) throws ResourceException {
        logger.log(Level.FINEST, "ManagedConnectionImpl associateConnection " +
            toString());
            
        addAmdocsConnection((AmdocsConnectionImpl)connection);
    }

    /**
     * Adds a ConnectionEventListener to this ManagedConnectionImpl.
     * <p>
     * Called from {@link ConnectionManagerImpl#allocateConnection ConnectionManagerImpl.allocateConnection}
     *
     * @param       listener        provided by the container.
     */
    public void addConnectionEventListener(ConnectionEventListener listener) {
        logger.log(Level.FINEST, "ManagedConnectionImpl addConnectionEventListener " +
            toString());
    
        connectionEventListeners.add(listener);
    }

    /**
     * Removes ConnectionEventListener to this ManagedConnectionImpl.
     *
     * @param       listener        provided by the container.
     */
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        logger.log(Level.FINEST, "ManagedConnectionImpl removeConnectionEventListener" +
            toString());
            
        connectionEventListeners.remove(listener);
    }

    /**
     * This is not implemented.
     *
     * @return      NotSupportedException.
     * @throws      javax.resource.ResourceException generic exception if
     *              operation fails due to an error condition.
     */
    public XAResource getXAResource() throws ResourceException {
        logger.log(Level.FINEST, "ManagedConnectionImpl getXAResource");
            
        throw new NotSupportedException();
    }

    /**
     * This is not implemented.
     *
     * @return      NotSupportedException
     * @throws      javax.resource.ResourceException generic exception if
     *              operation fails due to an error condition.
     */
    public LocalTransaction getLocalTransaction() throws ResourceException {
        logger.log(Level.FINEST, "ManagedConnectionImpl getLocalTransaction");
            
        throw new NotSupportedException();
    }

    /**
     * Creates a placeholder of ManagedConnectionImpl's meta data. User name is get from
     * this ManagedConnectionImpl's ManagedConnectionFactoryImpl, the other data
     * originates from static member variables in ManagedConnectionMetaDataImpl.
     *
     * @return      {@link ManagedConnectionMetaDataImpl ManagedConnectionMetaData}.
     * @throws      javax.resource.ResourceException generic exception if
     *              operation fails due to an error condition.
     */
    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        logger.log(Level.FINEST, "ManagedConnectionImpl getMetaData");
        
        return new ManagedConnectionMetaDataImpl(getManConFactImpl());
    }

    /**
     * Sets the logger to the same as the continer's.
     *
     * @param       out         log writer from contaner.
     * @throws      javax.resource.ResourceException generic exception if
     *              operation fails due to an error condition.
     */
    public void setLogWriter(PrintWriter out) throws ResourceException {
        printWriter = out;
    }

    /**
     * Gets the actual log writer.
     *
     * @return      log         writer to container.
     * @throws      javax.resource.ResourceException generic exception if
     *              operation fails due to an error condition.
     */
    public PrintWriter getLogWriter() throws ResourceException {
        return printWriter;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc=" called from ManagedConnectionFactory's matchManagedConnections ">
    /**
     * AppServer uses this method to check if new ManagedConnection needs to create.
     * If this method returns true then the needed physical connection already
     * exists to Amdocs and can be used.
     *
     * @param       conReqInf       {@link ConnectionRequestInfoImpl ConnectionRequestInfo}.
     * @return      true if Amdocs EJB entity name in
     *              {@link ConnectionRequestInfoImpl conReqInf} is the same as
     *              in this instance.
     */
    public boolean equals(ConnectionRequestInfo conReqInf) {
        ConnectionRequestInfoImpl conReqInfImpl =
            (ConnectionRequestInfoImpl)conReqInf;

        if (this.conReqInfImpl.getEntityName().equals(conReqInfImpl.getEntityName())) {
            logger.log(Level.FINEST, "ManagedConnectionImpl equals return true");
            
            return true;
        }
        else {
            logger.log(Level.FINEST, "ManagedConnectionImpl equals return false");
            
            return false;
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc=" called from AmdocsConnectionImpl ">
    /**
     * Add one handle to the list.
     * <p>
     * Called from: AmdocsConnectionImpl.associateConnection (when a ConnectionHandle
     * is relinked to another ManagedConnectionImpl), AmdocsConnectionImpl.getConnection,
     * AmdocsConnectionImpl.associateConnection
     *
     * @param       amdocsConnectionImpl      {@link AmdocsConnectionImpl handle}
     *                                        to add to handle list.
     */
    public void addAmdocsConnection(AmdocsConnectionImpl amdocsConnectionImpl) {
        connectionHandles.add(amdocsConnectionImpl);

        logger.log(Level.FINEST, "ManagedConnectionImpl addAmdocsConnection " +
            toString() +", current connection handle count " +connectionHandles.size());
    }

    /**
     * Removes one handle from the list.
     * <p>
     * Called from: ResourceAdapterImpl.stop - AmdocsConnectionImpl.invalidate,
     * associateConnection (when a ConnectionHandle is relinked to another
     * ManagedConnectionImpl), close.
     *
     * @param       amdocsConnectionImpl      {@link AmdocsConnectionImpl handle}
     *                                        to remove from handle list.
     */
    public void removeAmdocsConnection(AmdocsConnectionImpl amdocsConnectionImpl) {
        boolean removed = connectionHandles.remove(amdocsConnectionImpl);

        logger.log(Level.FINEST, "ManagedConnectionImpl removeAmdocsConnection " +
            toString() +", " +(removed ? "handle removed" : "no handle") +
            ", remaining handle count " +connectionHandles.size());
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc=" called from AmdocsGateway ">
    /**
     * Send an error event to container. The container removes this
     * ManagedConnectionImpl from the pool as a consequence.
     *
     * @param       errorMessage        serves debug, this message will be written
     *                                  into the log.
     */
    public void error(String errorMessage) {
        logger.log(Level.FINEST, "ManagedConnectionImpl error " +toString() +
            ", " +errorMessage);
            
        ConnectionEvent errorEvent =
            new ConnectionEvent(this, ConnectionEvent.CONNECTION_ERROR_OCCURRED);

        Iterator<ConnectionEventListener> connectionEventListenersIter =
            connectionEventListeners.iterator();

        while (connectionEventListenersIter.hasNext()) {
            ConnectionEventListener connectionEventListener =
                connectionEventListenersIter.next();

            connectionEventListener.connectionErrorOccurred(errorEvent);
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc=" business methods ">
    /**
     * This method provides the Amdocs EJB's remote IF. After a cast on client side to
     * the desired EJB type business methods can be called.
     * <p>
     * Calls {@link AmdocsGateway#getAPI AmdocsGateway.getAPI}.
     * <p>
     * If {@link AmdocsGateway#getAPI AmdocsGateway.getAPI} returns null then
     * calls {@link ManagedConnectionImpl#error ManagedConnectionImpl.error} and
     * raises a ResourceException.
     *
     * @return      Amdocs EJB's remote IF.
     * @throws      javax.resource.ResourceException generic exception if
     *              operation fails due to an error condition.
     */
    public Object getAmdocsRef() throws ResourceException {
        logger.log(Level.FINEST, "ManagedConnectionImpl getAmdocsRef " +
            toString() +" - AmdocsGateway " +amdocsGateway.toString());
        
        if (amdocsGateway.getAPI(" from ManagedConnectionImpl getAmdocsRef ") != null) {
            lastUsageDate = new Date(System.currentTimeMillis());
            return amdocsGateway.getAPI(" from ManagedConnectionImpl getAmdocsRef ");
        }
        else {
            // remove this ManagedConnectionImpl from pool - amdocs.getAPI = null
            error("ManagedConnectionImpl getAmdocsRef " +toString() +
                " error, getAmdocsRef = null");
            // throw exception to client
            throw new ResourceException("no valid Amdocs reference");
        }
    }

    /**
     * When this method is called on client side then AppServer calls cleanup
     *
     * @param       amdocsConnectionImpl        {@link AmdocsConnectionImpl AmdocsConnectionImpl}
     *                                          on which close was invoked.
     * @throws ResourceException exception happened during close.
     */
    public void close(AmdocsConnectionImpl amdocsConnectionImpl)
    throws ResourceException {

        logger.log(Level.INFO, "ManagedConnectionImpl close " +toString() +
            ", " +amdocsConnectionImpl.toString());
            
        ConnectionEvent closeEvent =
            new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);

        Iterator<ConnectionEventListener> connectionEventListenersIter =
            connectionEventListeners.iterator();

        while (connectionEventListenersIter.hasNext()) {
            ConnectionEventListener connectionEventListener =
                connectionEventListenersIter.next();

            connectionEventListener.connectionClosed(closeEvent);
        }
    }
    // </editor-fold>

    /**
     * Shows how many parallel sessions exists to an Amdocs EJB on the physical
     * connection.
     * <p>
     * Called from AmdocsConnectionImpl.CTOR, AmdocsConnectionImpl.close, toString.
     *
     * @return      number of handles (sessions) to a specific Amdocs EJB referenced
     *              by this ManagedConnectionImpl instance.
     */
    public int getHandleCount() {
        return connectionHandles.size();
    }

    /**
     * Provides information about the data in this instance.
     *
     * @return      string representation of the instance's data.
     */
    @Override
    public String toString() {
        return "[creationDate " +creationDate +
            ", lastUsageDate " +(lastUsageDate != null ? lastUsageDate : "not used")+
            ", ManagedConnectionFactoryImpl " +getManConFactImpl().toString() +
            ", ConnectionRequestInfoImpl " +conReqInfImpl.toString() +
            ", handle count " +getHandleCount() +", " +(amdocsGateway == null ? "invalid" :
            (amdocsGateway.getAPI(" from ManagedConnImpl toString ") != null ? "valid" : "invalid")) +"]";
    }

    /**
     * Currently not used.
     *
     * @return      all current connection handles.
     */
    private String dumpConnectionHandles() {
        String result = "";

        Iterator<AmdocsConnectionImpl> connections = connectionHandles.iterator();
        while (connections.hasNext()) {
            AmdocsConnectionImpl amdocsConnectionImpl = connections.next();
            result += amdocsConnectionImpl.toString() +System.getProperty("line.separator");
        }

        return result;
    }

    /**
     * Gets the ManagedConnectionFactoryImpl which created this instance of
     * ManagedConnectionImpl. ManagedConnectionFactoryImpl has a member variable which
     * contains the set of created ManagedConnectionImpls.
     * <p>
     * Called from CTOR, getConnection, getMetaData, toString, AmdocsGateway.CTOR,
     * AmdocsGateway.setLoginContext, AmdocsGateway.setAPI,
     * AmdocsGateway.createAmdCtxRefreshTimer, AmdocsGateway.toString
     *
     * @return      {@link ManagedConnectionFactoryImpl ManagedConnectionFactoryImpl}
     *              that created this ManagedConnectionImpl.
     */
    public ManagedConnectionFactoryImpl getManConFactImpl() {
        return manConFactImpl;
    }

    /**
     * Gets the creation date of this ManagedConnecationImpl instance.
     * <p>
     * Called from ManagedConnectionFactoryIMpl.getInvalidConnections
     *
     * @return      date of creation this ManagedConnectionImpl instance.
     */
    public Date getCreationDate() {
        return creationDate;
    }

    /**
     * Gets the last usage date of this ManagedConnecationImpl instance.
     * <p>
     * Called from ManagedConnectionFactoryIMpl.getInvalidConnections
     *
     * @return      date of last usage of this ManagedConnectionImpl instance.
     */
    public Date getLastUsageDate() {
        return lastUsageDate;
    }
    
}
