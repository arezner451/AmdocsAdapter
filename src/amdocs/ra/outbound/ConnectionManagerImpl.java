package amdocs.ra.outbound;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnectionFactory;

/*
 * ConnectionManagerImpl.java
 *
 * Created on 2006. április 13., 9:46
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 * This class is used only in non-managed environment. In a managed environment
 * this class is provided by the container. The instance of this class is creted
 * in AmdocsConnectionFactoryImpl.CTOR and is stored in a member variable.
 *
 * @author attila.rezner
 */
public class ConnectionManagerImpl implements ConnectionManager, ConnectionEventListener {
    /** @see java.util.logging.Logger */
    private static Logger logger = 
        Logger.getLogger("javax.enterprise.resource.resourceadapter");
    /** pool of {@link amdocs.ra.outbound.ManagedConnectionImpl managedConnection}s */
    private Set<ManagedConnectionImpl> manConnPool;
    /**
     * timer that checks periodically all
     * {@link ManagedConnectionImpl ManagedConnectionImpl}s in the pool for
     * validity.
     */
    private Timer managedConnPoolTimer;

    /** Creates a new instance of ConnectionManagerImpl */
    public ConnectionManagerImpl() {
        manConnPool = new HashSet<ManagedConnectionImpl>();
        /**
         * this timer periodically checks every managedConnectionImpl's amdocs
         * member variable if null then removes managedConnectionImpl from pool
         */
        createManagedConnPoolTimer();
    }

    // <editor-fold defaultstate="collapsed" desc=" inherited from ConnectionManager ">
    /**
     * Called from AmdocsConnectionFactory.createConnection
     *
     * @param   mcf             {@link ManagedConnectionFactoryImpl ManagedConnectionFactoryImpl}
     * @param   cxRequestInfo   {@link ConnectionRequestInfoImpl ConnectionRequestInfoImpl}
     * @return                  {@link AmdocsConnectionImpl AmdocsConnectionImpl}.
     * @throws ResourceException exception thrown.
     */
    public Object allocateConnection(
        ManagedConnectionFactory mcf, ConnectionRequestInfo cxRequestInfo)
    throws ResourceException {

        logger.log(Level.INFO, "ConnectionManagerImpl allocateConnection(" +
            "ManagedConnectionFactoryImpl " +
                ((ManagedConnectionFactoryImpl)mcf).toString() +
            ", ConnectionRequestInfoImpl " +
                ((ConnectionRequestInfoImpl)cxRequestInfo).toString() +")");
        /**
         * ManagedConnection represents a physical connection to EIS.
         */
        ManagedConnectionImpl mcImpl = (ManagedConnectionImpl)
            mcf.matchManagedConnections(manConnPool, null, cxRequestInfo);
        /**
         * if no suitable managedConnection found in the pool
         * then create a new managedConenction
         */
        if (mcImpl == null) {
            mcImpl = (ManagedConnectionImpl)
                mcf.createManagedConnection(null, cxRequestInfo);
            // add new managedConnectionImpl to the pool
            manConnPool.add(mcImpl);
        }
        /**  */
        mcImpl.addConnectionEventListener(this);
        /**
         * gets a connection handle to the physical EIS connection.
         * creates instance of AmdocsConnectionImpl and returns as AmdocsConnection.
         */
        return mcImpl.getConnection(null, cxRequestInfo);
    }
    // </editor-fold>

    /**
     * Periodically checks if {@link ManagedConnectionImpl managedConnection}
     * is invalid (amdocs != null), and removes from the pool if is.
     */
    private void createManagedConnPoolTimer() {
        managedConnPoolTimer = new Timer();
        managedConnPoolTimer.schedule(new TimerTask() {
            public void run() {
                /**
                 * makes possible to call toString from the Timer thread
                 * (without this if the toString is called from Timer thread
                 * then toString is invoked on the Timer)
                 */
                logger.log(Level.FINEST, "ConnectionManagerImpl timer run " +
                    _toString());
                    
                Iterator<ManagedConnectionImpl> manConnImpls = manConnPool.iterator();
                while (manConnImpls.hasNext()) {
                    ManagedConnectionImpl manConnImpl = manConnImpls.next();

                    if (manConnImpl.amdocsGateway == null) {
                        manConnPool.remove(manConnImpl);
                    }
                }
            }
        // first delay , refresh
        }, 10000, 10000);
    }

    /**
     * Destroys all {@link ManagedConnectionImpl ManagedConnectionImpl}.
     * <p>
     * Called from client to close all.
     */
    public void destroyManagedConns() {
        logger.log(Level.FINEST, "ConnectionManagerImpl destroyManagedConns");
            
        Iterator<ManagedConnectionImpl> manConnImpls = manConnPool.iterator();
        while (manConnImpls.hasNext()) {
            ManagedConnectionImpl manConnImpl = manConnImpls.next();
            try {
                manConnImpl.destroy();
            }
            catch (ResourceException ex) {
                ex.printStackTrace();
            }
            finally {
                manConnPool.remove(manConnImpl);
            }
            cancelTimer();
        }
    }

    /** Calls destroy on all {@link ManagedConnectionImpl ManagedConnectionImpl}
     * 
     * @param connectionSpecImpl connection parameters defining API to destroy.
     *
     */
    public void destroyManagedConn(ConnectionSpecImpl connectionSpecImpl) {
        logger.log(Level.FINEST, "ConnectionManagerImpl destroyManagedConn");
            
        ConnectionRequestInfoImpl connRequestInfImpl = new ConnectionRequestInfoImpl(
            connectionSpecImpl.getEntityName(), connectionSpecImpl.getHomeClassName(),
            connectionSpecImpl.getRemoteClassName());

        Iterator<ManagedConnectionImpl> manConnImpls = manConnPool.iterator();
        while (manConnImpls.hasNext()) {
            ManagedConnectionImpl manConnImpl = manConnImpls.next();

            if (manConnImpl.equals((ConnectionRequestInfo)connRequestInfImpl)) {
                try {
                    manConnImpl.destroy();
                }
                catch (ResourceException ex) {
                    ex.printStackTrace();
                }
                finally {
                    manConnPool.remove(manConnImpl);

                    logger.log(Level.FINEST, "ConnectionManagerImpl destroyManagedConn " +
                        "managedConnectionImpl [" +manConnImpl.toString() +"] destoyed");
                    
                    break;
                }
            }
        }
    }

    /**
     * Reset {@link ManagedConnectionImpl ManagedConnectionImpl}.
     * <p>
     * Called from client when exception occurs. The {@link ManagedConnectionImpl managedConn}
     * stays in the pool.
     */
    public void cleanupManagedConns() {
        logger.log(Level.FINEST, "ConnectionManagerImpl cleanupManagedConns");
            
        Iterator<ManagedConnectionImpl> manConnImpls = manConnPool.iterator();
        while (manConnImpls.hasNext()) {
            ManagedConnectionImpl manConnImpl = manConnImpls.next();
            try {
                manConnImpl.cleanup();
            }
            catch (ResourceException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**  
     * Cleans the managedConnection instance.
     * 
     * @param connectionSpecImpl connection parameters defining API to cleanup.
     */
    public void cleanupManagedConn(ConnectionSpecImpl connectionSpecImpl) {
        logger.log(Level.FINEST, "ConnectionManagerImpl cleanupManagedConn");
            
        ConnectionRequestInfoImpl connRequestInfImpl = new ConnectionRequestInfoImpl(
            connectionSpecImpl.getEntityName(), connectionSpecImpl.getHomeClassName(),
            connectionSpecImpl.getRemoteClassName());

        Iterator<ManagedConnectionImpl> manConnImpls = manConnPool.iterator();
        while (manConnImpls.hasNext()) {
            ManagedConnectionImpl manConnImpl = manConnImpls.next();

            if (manConnImpl.equals((ConnectionRequestInfo)connRequestInfImpl)) {
                try {
                    manConnImpl.cleanup();
                }
                catch (ResourceException ex) {
                    ex.printStackTrace();
                }
                finally {
                    logger.log(Level.FINEST, "ConnectionManagerImpl cleanupManagedConn " +
                        "managedConnectionImpl [" +manConnImpl.toString() +"] removed");
                        
                    break;
                }
            }
        }
    }

    /** Prints all {@link ManagedConnectionImpl ManagedConnectionImpls} into the log */
    public void dumpManagedConnections() {
        String result = "";
        int ind = 0;

        Iterator<ManagedConnectionImpl> manConnImpls = manConnPool.iterator();
        while (manConnImpls.hasNext()) {
            ManagedConnectionImpl manConnImpl = manConnImpls.next();
            result = "[" +(++ind) +" " +manConnImpl.toString() +"]";
        }
        logger.log(Level.FINEST, "ConnectionManagerImpl dumpManagedConnections [" +
            result +"]");
    }

    /**
     * Cancel timer which removes invalid
     * {@link ManagedConnectionImpl ManagedConnectionImpl}s from the pool.
     */
    public void cancelTimer() {
        managedConnPoolTimer.cancel();
    }

    /**
     * Makes possible to call {@link #toString toString} from the Timer thread
     * (without this if the toString is called from Timer thread then toString is
     * invoked on the Timer)
     *
     * @return      string representation of the instance's data.
     */
    public String _toString() {
        String result = "";
        int ind = 0;

        Iterator<ManagedConnectionImpl> manConnImpls = manConnPool.iterator();
        while (manConnImpls.hasNext()) {
            ManagedConnectionImpl manConnImpl = manConnImpls.next();
            result = "[" +(++ind) +" " +manConnImpl.toString() +"]";
        }
        return result;
    }

    /**
     * Put back MamagedConnectionImpl into the pool...
     * 
     * @param event event representing close event of managed connection, 
     * so put back to pool.
     * 
     */
    public void connectionClosed(ConnectionEvent event) {
        AmdocsConnectionImpl amdocsConnectionImpl =
            (AmdocsConnectionImpl) event.getConnectionHandle();

        ManagedConnectionImpl managedConnectionImpl =
            amdocsConnectionImpl.getManagedConnection();

        manConnPool.add(managedConnectionImpl);
    }

    /**
     * 
     * @param event event representing transaction begin.
     */
    public void localTransactionStarted(ConnectionEvent event) {
    }

    /**
     * 
     * @param event event representing transaction commit.
     */
    public void localTransactionCommitted(ConnectionEvent event) {
    }

    /**
     * 
     * @param event event representing transaction rollback.
     */
    public void localTransactionRolledback(ConnectionEvent event) {
    }

    /**
     * 
     * @param event event representing transaction error.
     */
    public void connectionErrorOccurred(ConnectionEvent event) {
    }

}
