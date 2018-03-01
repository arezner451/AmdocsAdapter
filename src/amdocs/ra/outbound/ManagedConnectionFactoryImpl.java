package amdocs.ra.outbound;

import java.io.PrintWriter;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ValidatingManagedConnectionFactory;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
/*
 * ManagedConnectionFactoryImpl.java
 *
 * Created on 2006. április 4., 14:29
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 * ManagedConnectionFactory JavaBean represents outbound connection to an EIS
 * instance from a ResourceAdapter instance.
 *
 * @author attila.rezner
 */
public class ManagedConnectionFactoryImpl
implements ManagedConnectionFactory, ValidatingManagedConnectionFactory {
    /** @see java.util.logging.Logger */
    private static Logger logger = 
        Logger.getLogger("javax.enterprise.resource.resourceadapter");
    /**  */
    transient private PrintWriter printWriter;
    /** creation date of this ManagedConnectionFactoryImpl */
    private Date creationDate;
    /**
     * {@link ManagedConnectionImpl ManagedConnection}s created by this
     * connection factory instance
     */
    private Set<ManagedConnectionImpl> managedConnections = null;

    // <editor-fold defaultstate="collapsed" desc=" EIS instance bean properties ">
    /** required to connect to EIS (Amdocs environment) */
    private String host;
    /** required to connect to EIS (Amdocs environment) */
    private Integer port;
    /** required to connect to EIS (Amdocs environment) */
    private String environment;
    /** required to connect to EIS (Amdocs environment) */
    private String userName;
    /** required to connect to EIS (Amdocs environment) */
    private String password;
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc=" adapter parameters ">
    /**
     * Time to live of this connection <i>in minutes</i>.
     */
    private int amdCtxRefresh = 30;
    /**
     * Time to live if no request on this conenction <i>in minutes</i>.
     */
    private int amdEjbRefRefresh = 10;
    // </editor-fold>
    /**
     * reference to AmdocsGateway wich has static method that needs to be
     * called after invalidation if no valid connections left in the pool.
     */
    AmdocsGateway amdocsGateway;

    /**
     * Creates a new instance of ManagedConnectionFactoryImpl.
     * <p>
     * Called by container after ResourceAdapterImpl.start.
     */
    public ManagedConnectionFactoryImpl() {
        creationDate = new Date();

        logger.log(Level.FINEST, "ManagedConnectionFactoryImpl CTOR, creationDate " +
            creationDate);
            
        managedConnections = new HashSet<ManagedConnectionImpl>();
    }

    // <editor-fold defaultstate="collapsed" desc=" inherited from ManagedConnectionFactory ">
    /**
     * Creates a {@link AmdocsConnectionFactoryImpl AmdocsConnectionFactoryImpl}
     * instance.
     * <p>
     * Called by container after AmdocsConnectionFactoryImpl.CTOR.
     *
     * @param       cxManager       got from AppServer.
     * @return      a newly created {@link AmdocsConnectionFactoryImpl AmdocsConnectionFactoryImpl}.
     * @throws      javax.resource.ResourceException generic exception
     *              if operation fails due to an error condition.
     */
    public Object createConnectionFactory(ConnectionManager cxManager)
    throws ResourceException {

        logger.log(Level.FINEST, "ManagedConnectionFactoryImpl createConnectionFactory" +
            "-ConnectionManager from AppServer " +toString());

        AmdocsConnectionFactoryImpl amdConnFactImpl =
            new AmdocsConnectionFactoryImpl(this, cxManager);

        return amdConnFactImpl;
    }

    /**
     * Creates a {@link AmdocsConnectionFactoryImpl AmdocsConnectionFactoryImpl}
     * instance. The {@link ConnectionManagerImpl ConnectionManager} is provided
     * in ResourceAdapter.
     * <p>
     * Called by container after AmdocsConnectionFactoryImpl.CTOR.
     *
     * @return    a newly created {@link AmdocsConnectionFactoryImpl AmdocsConnectionFactoryImpl}.
     * @throws    javax.resource.ResourceException generic exception if operation
     *            fails due to an error condition.
     */
    public Object createConnectionFactory() throws ResourceException {
        logger.log(Level.FINEST, "ManagedConnectionFactoryImpl createConnectionFactory" +
            "-ConnectionManager by Adapter " +toString());

        AmdocsConnectionFactoryImpl amdConnFactImpl =
            new AmdocsConnectionFactoryImpl(this, null);

        return amdConnFactImpl;
    }

    /**
     * Creates a physical Connection to the EIS instance.
     * <p>
     * Called from ConnectionManager.allocateConnection.
     *
     * @param   subject         got from container.
     * @param   cxRequestInfo   {@link ConnectionRequestInfoImpl cxRequestInfo}.
     * @return                  an existing or a newly created
     *                          {@link ManagedConnectionImpl ManagedConnectionImpl}
     *                          if not found a proper.
     * @throws                  javax.resource.ResourceException generic exception
     *                          if operation fails due to an error condition.
     */
    public ManagedConnection createManagedConnection(
        Subject subject, ConnectionRequestInfo cxRequestInfo)
    throws ResourceException {

        logger.log(Level.INFO, "ManagedConnectionFactoryImpl createManagedConnection " +
            toString() +", ConnectionRequestInfoImpl " +
            ((ConnectionRequestInfoImpl)cxRequestInfo).toString());

        ManagedConnectionImpl manConnImpl =
            new ManagedConnectionImpl(this, subject, cxRequestInfo);

        managedConnections.add(manConnImpl);

        /**
         * Serves to make possible call of static method in AmdocsGateway from
         * getInvalidConnections.
         */
        if (amdocsGateway == null) {
            amdocsGateway = manConnImpl.amdocsGateway;
        }

        return manConnImpl;
    }

    /**
     * If this method throw NotSupportedException then AppServer avoids pooling
     * connections. This method serves connection pooling. This method makes
     * possible to decide that new {@link ManagedConnectionImpl ManagedConnectionImpl}
     * (physical connection) needed to EIS (Amdocs), or already exists a proper.
     *
     * @param   connectionSet       got from container, contains the potentional
     *                              {@link ManagedConnectionImpl ManagedConnectionImpls}.
     * @param   subject             got from container.
     * @param   cxRequestInfo       placeholder to the JNDI name of Amdocs EJB to
     *                              which connection needed
     *                              {@link ConnectionRequestInfoImpl cxRequestInfo}.
     * @return                      an existing
     *                              {@link ManagedConnectionImpl ManagedConnectionImpl}
     *                              or null if not found a proper.
     * @throws                      javax.resource.ResourceException generic exception
     *                              if operation fails due to an error condition.
     */
    public ManagedConnection matchManagedConnections(
        Set connectionSet, Subject subject, ConnectionRequestInfo cxRequestInfo)
    throws ResourceException {

        ConnectionRequestInfoImpl conReqInfImpl = (ConnectionRequestInfoImpl)cxRequestInfo;

        logger.log(Level.FINE, "ManagedConnectionFactoryImpl matchManagedConnection, " +
            toString() +", ConnectionRequestInfoImpl " +conReqInfImpl.toString());
            
        // <editor-fold defaultstate="collapsed" desc=" security handling ">
        PasswordCredential passwordCredential =
            new PasswordCredential(userName, password.toCharArray());

        passwordCredential.setManagedConnectionFactory(this);
        // </editor-fold>
        Iterator connections = connectionSet.iterator();
        while (connections.hasNext()) {
            ManagedConnectionImpl manConImpl = (ManagedConnectionImpl)connections.next();
            /**
             * true if ManagedConnectionImpl.getEntityName() = conReqInfImpl.getEntityName()
             * and if managedConnectionImpl has a non null amdocs reference -which
             * means valid connection to Amdocs
             */
            if (manConImpl.equals(conReqInfImpl)) { //&& manConImpl.amdocs.getAPI() != null) {
                logger.log(Level.FINE, "ManagedConnectionFactoryImpl.matchManagedConnections" +
                    " found proper ManagedConnectionImpl " +manConImpl.toString() +
                    ", - AmdocsGateway " +manConImpl.amdocsGateway.toString());
                    
                return manConImpl;
            }
        }
        
        logger.log(Level.FINE, "ManagedConnectionFactoryImpl.matchManagedConnections" +
            " not found proper managed connection");
            
        return null;
    }

    /**
     * Sets the logger to the same as the continer's.
     *
     * @param       out         log writer from contaner.
     * @throws                  javax.resource.ResourceException generic exception
     *                          if operation fails due to an error condition.
     */
    public void setLogWriter(PrintWriter out) throws ResourceException {
        this.printWriter = out;
    }

    /**
     * Gets the actual log writer.
     *
     * @return      log writer to container.
     * @throws      javax.resource.ResourceException generic exception if operation
     *              fails due to an error condition.
     */
    public PrintWriter getLogWriter() throws ResourceException {
        return printWriter;
    }

    // <editor-fold defaultstate="collapsed" desc=" AppServer connection-pool management ">
    /**
     * The result is based on complete set of config parameters that make
     * ManagedConnectionFactoryImpl unique and specific to an EIS instance.
     *
     * @return      hash code for the {@link ManagedConnectionFactoryImpl ManagedConnectionFactoryImpl} instance.
     */
    public int hashCode() {
        logger.log(Level.FINEST, "ManagedConnectionFactoryImpl hashCode");
        
        String portStr = Integer.toString(port);
        return (host +portStr +environment +userName +password).hashCode();
    }

    /**
     * Check if this ManagedConnectionFactoryImpl is equal to another
     * ManagedConnectionFactoryImpl passed in obj based on complete set of config
     * parameters that make ManagedConnectionFactoryImpl unique and specific to
     * an EIS instance.
     * <p>
     * Called by container after ManagedConnectionFactoryImpl createConnectionFactory.
     *
     * @param       obj         another {@link ManagedConnectionFactoryImpl ManagedConnectionFactoryImpl}.
     * @return                  true if this
     *                          {@link ManagedConnectionFactoryImpl ManagedConnectionFactoryImpl}
     *                          and the one got in {@link ManagedConnectionFactoryImpl obj}
     *                          are the same.
     */
    public boolean equals(Object obj) {
        ManagedConnectionFactoryImpl managedConnectionFactoryImpl =
            (ManagedConnectionFactoryImpl)obj;

        if (this.host.equals(managedConnectionFactoryImpl.host) &&
            this.port == managedConnectionFactoryImpl.port &&
            this.environment.equals(managedConnectionFactoryImpl.environment) &&
            this.userName.equals(managedConnectionFactoryImpl.userName) &&
            this.password.equals(managedConnectionFactoryImpl.password)
        ) {

            logger.log(Level.FINEST, "ManagedConnectionFactoryImpl equals return " +
                "true, " +this.toString() +"==" +managedConnectionFactoryImpl.toString());
                
            return true;
        }
        else {
            logger.log(Level.FINEST, "ManagedConnectionFactoryImpl equals return " +
                "false, " +this.toString() +"==" +managedConnectionFactoryImpl.toString());
                
            return false;
        }
    }
    // </editor-fold>
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc=" bean property get/set methods ">    
    /**
     * EIS (Amdocs) instance property.
     * <p>
     * Called in AmdocsGateway.setAPI.
     *
     * @return      EIS instance's host name
     */
    public String getHost() {
        return host;
    }

    /**
     * EIS (Amdocs) instance property.
     * <p>
     * Called by continer to set value supplied during deploy.
     *
     * @param       host        EIS instance's host name.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * EIS (Amdocs) instance property. Called in AmdocsGateway.setAPI.
     *
     * @return      EIS instance's port.
     */
    public Integer getPort() {
        return port;
    }

    /**
     * EIS (Amdocs) instance property.
     * <p>
     * Called by continer to set value supplied during deploy.
     *
     * @param       port        EIS instance's port.
     */
    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * EIS (Amdocs) instance property.
     * <p>
     * Called in AmdocsGateway.setLoginContext.
     *
     * @return      EIS instance's port.
     */
    public String getEnvironment() {
        return environment;
    }

    /**
     * EIS (Amdocs) instance property.
     * <p>
     * Called by continer to set value supplied during deploy.
     *
     * @param       environment         EIS instance's environment name.
     */
    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    /**
     * EIS (Amdocs) instance property.
     * <p>
     * Called in AmdocsGateway.setLoginContex, ManagedConnectionImpl CTOR,
     * ManagedConnectionImpl.getConnection.
     *
     * @return      EIS instance's port.
     */
    public String getUserName() {
        return userName;
    }

    /**
     * EIS (Amdocs) instance property.
     * <p>
     * Called by continer to set value supplied during deploy.
     *
     * @param       userName            EIS instance's user name.
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * EIS (Amdocs) instance property.
     * <p>
     * Called in AmdocsGateway.setLoginContex, ManagedConnectionImpl CTOR,
     * ManagedConnectionImpl.getConnection.
     *
     * @return      EIS instance's port.
     */
    public String getPassword() {
        return password;
    }

    /**
     * EIS (Amdocs) instance property.
     * <p>
     * Called by continer to set value supplied during deploy.
     *
     * @param       password        EIS instance's password.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * This parameter serves connector parameterization. Time to live of this 
     * connection <i>in minutes</i>.
     * <p>
     * Called by container to set value supplied during deploy.
     *
     * @return      Time to live of this connection <i>in minutes</i>.
     */
    public Integer getAmdCtxRefresh() {
        return new Integer(amdCtxRefresh);
    }

    /**
     * This parameter serves connector parameterization. Time to live of this 
     * connection <i>in minutes</i>.
     * <p>
     * Called by container to set value supplied during deploy.
     *
     * @param       amdCtxRefresh       Time to live of this connection <i>in minutes</i>.
     */
    public void setAmdCtxRefresh(Integer amdCtxRefresh) {
        this.amdCtxRefresh = amdCtxRefresh.intValue();
    }

    /**
     * This parameter serves connector parameterization. Time to live if no 
     * request on this conenction <i>in minutes</i>.
     * <p>
     * Called by container to set value supplied during deploy.
     *
     * @return      Time to live if no request on this conenction <i>in minutes</i>.
     */
    public Integer getAmdEjbRefRefresh() {
        return new Integer(amdEjbRefRefresh);
    }

    /**
     * This parameter serves connector parameterization. Time to live if no 
     * request on this conenction <i>in minutes</i>.
     * <p>
     * Called by container to set value supplied during deploy.
     *
     * @param       amdEjbRefRefresh    Time to live if no request on this 
     *                                  conenction <i>in minutes</i>.
     */
    public void setAmdEjbRefRefresh(Integer amdEjbRefRefresh) {
        this.amdEjbRefRefresh = amdEjbRefRefresh.intValue();
    }
    // </editor-fold>

    /**
     * Provides information about the data in this instance.
     *
     * @return      string representation of the instance's data.
     */
    @Override
    public String toString() {
        String portStr = Integer.toString(port);
        return "[creationDate " +creationDate +
            ", [" +host +"," +portStr + "," +environment +"," +userName + "," +password +"]]";
   }

    // <editor-fold defaultstate="collapsed" desc=" inherited from ValidatingManagedConnectionFactory ">
    /**
     * TODO: meg lehetne oldani, hogy egy ido után mindenképpen kidobódjaon egy
     * fizikai kapcsolat... (a ManagedConnectionImpl-ben megvan a kreálási ido)
     */
    /**
     * Needed to support validation of ManagedConnectionImpl instances.
     * <p>
     * Container runs in every X minutes. X is set on Admin Console.
     * A ManagedConnectionImpl is invalid if its amdocs member variable's getAPI
     * returns null.
     *
     * @param       connectionSet       actual {@link ManagedConnectionImpl ManagedConnectionImpls} that this
     *                                  ManagedConnectionFactoryImpl created.
     * @return      set of invalid connections : {@link ManagedConnectionImpl ManagedConnectionImpls}.
     * @throws      javax.resource.ResourceException generic exception if operation
     *              fails due to an error condition.
     */
    @SuppressWarnings("unchecked")
    public Set getInvalidConnections(Set connectionSet) throws ResourceException {
        // log
        logger.log(Level.FINEST, "ManagedConnectionFactoryImpl getInvalidConnections " +
            toString());
        // set of managed connections that needs to be destroyed
        Set<ManagedConnectionImpl> invConnSet = new HashSet<ManagedConnectionImpl>();
        // iterator on set of managed connections -provided by server to validate
        Iterator<ManagedConnectionImpl> managedConns = connectionSet.iterator();
        while (managedConns.hasNext()) {
            ManagedConnectionImpl manConnImpl = managedConns.next();

            long manConnImplDuration =
                System.currentTimeMillis() -manConnImpl.getCreationDate().getTime();

            long manConnImplSinceLastUse =
                System.currentTimeMillis() -manConnImpl.getLastUsageDate().getTime();

            if (manConnImplDuration > amdCtxRefresh *60000 ||
                manConnImplSinceLastUse > amdEjbRefRefresh *60000) {

                logger.log(Level.FINEST, "ManagedConnectionFactoryImpl getInvalidConnections " +
                    toString() +" -added " +manConnImpl.toString() +" to invalid connections");
            
                invConnSet.add(manConnImpl);
            }
        }
        // if no physical connection to Amdocs then logout from UAMS
        if (connectionSet.size() == invConnSet.size() && amdocsGateway != null) {
            amdocsGateway.releaseLoginContext();
        }
        // log
        logger.log(Level.FINEST, "ManagedConnectionFactoryImpl getInvalidConnections " +
            toString() +", invalidated " +invConnSet.size() +
            " ManagedConnectionImpl from " +connectionSet.size());
    
        return invConnSet;
    }
    // </editor-fold>

    /**
     * When a {@link ManagedConnectionImpl ManagedConnectionImpl} destroys
     * -which was created by this ManagedConnectionFactoryImpl- then this method
     * is called and the caller {@link ManagedConnectionImpl ManagedConnectionImpl}
     * removes oneself from the list.
     * <p>
     * Called in ManagedConnectionImpl.destroy.
     *
     * @return      set of {@link ManagedConnectionImpl ManagedConnectionImpls}
     *              created by this instance.
     */
    public Set<ManagedConnectionImpl> getManagedConnsOfFactory() {
        return managedConnections;
    }

}
