package amdocs.ra.outbound;

import amdocs.ra.client.AmdocsConnection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.resource.ResourceException;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.cci.LocalTransaction;
import javax.resource.spi.ManagedConnection;
/*
 * AmdocsConnectionImpl.java
 *
 * Created on 2006. május 9., 12:00
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 * An instance of this class is a handle to a physical connection represented by
 * a ManagedConnectionImpl instance.
 *
 * @author attila.rezner
 */
public class AmdocsConnectionImpl implements AmdocsConnection {
    /** @see java.util.logging.Logger */
    private static Logger logger = 
        Logger.getLogger("javax.enterprise.resource.resourceadapter");
    /**
     * reference to the physical connection -
     * {@link ManagedConnectionImpl ManagedConnectionImpl} - to which this
     * AmdocsConnectionImpl instance is a handle.
     */
    private ManagedConnectionImpl manConImpl;
    /** creation date of this AmdocsConnectionImpl */
    private Date creationDate;
    /** */
    private volatile boolean isClosed = false;

    /**
     * Currently not used.
     */
    public AmdocsConnectionImpl() {
    }

    /**
     * Creates a new instance of AmdocsConnectionImpl. Called from
     * ManagedConnection.getConnection.
     *
     * @param   mc      {@link ManagedConnectionImpl ManagedConnection}.
     */
    public AmdocsConnectionImpl(ManagedConnection mc) {
        manConImpl = (ManagedConnectionImpl)mc;

        logger.log(Level.INFO, "AmdocsConnectionImpl CTOR(" +
            "ManagedConnectionImpl " +manConImpl.toString() +")");

        creationDate = new Date();
    }

    /**
     * This method can be used if a reference required to this handle's
     * ManagedConnectionImpl.
     *
     * @return ManagedConnectionImpl of this handle.
     */
    public ManagedConnectionImpl getManagedConnection() {
        logger.log(Level.FINEST, "AmdocsConnectionImpl getManagedConnection " +
            toString());

        return manConImpl;
    }

    /**
     * This method is not implemented.
     *
     * @return      ResourceException.
     * @throws      javax.resource.ResourceException generic exception if operation
     *              fails due to an error condition.
     */
    public LocalTransaction getLocalTransaction() throws ResourceException {
        logger.log(Level.FINEST, "AmdocsConnectionImpl getLocalTransaction");
            
        throw new ResourceException("NO_TRANSACTION");
    }

    /**
     * This method returns with a placeholder of EIS (Amdocs) data to which this
     * instance is a handle.
     *
     * @return      a newly created ConnectionMetaDataImpl.
     * @throws      javax.resource.ResourceException generic exception if operation
     *              fails due to an error condition.
     */
    public ConnectionMetaData getMetaData() throws ResourceException {
        logger.log(Level.FINEST, "AmdocsConnectionImpl getMetaData");

        return new ConnectionMetaDataImpl(manConImpl);
    }

    /**
     * This method replaces the ManagedConnectionImpl to which this instance is a
     * handle from the actual ManagedConnectionImpl to the one got in mc.
     *
     * @param       mc      {@link ManagedConnectionImpl ManagedConnection} to
     *                      which this handle should be associated.
     * @throws              javax.resource.ResourceException generic exception
     *                      if operation fails due to an error condition.
     */
    public void associateConnection(ManagedConnection mc)
    throws ResourceException {

        logger.log(Level.FINEST, "AmdocsConnectionImpl associateConnection" +
            toString() +"[" +((ManagedConnectionImpl)mc).toString() +"]");
        /**
         * if ManagedConnectionImpl member variable null then
         * this handle was invalidated previously
         */
        if (manConImpl == null) {
            throw new ResourceException("INVALID_CONNECTION_HANDLE");
        }
        else {
            manConImpl.removeAmdocsConnection(this);
            ((ManagedConnectionImpl)mc).addAmdocsConnection(this);
            manConImpl = (ManagedConnectionImpl)mc;
        }
    }

    /**
     * Called from ResourceAdapterImpl stop during undeploy or server shutdown. The
     * physical connection is not affected. This handle removes oneself from its
     * ManagedConnectionImpl's set of handles.
     */
    public void invalidate() {
        logger.log(Level.FINEST, "AmdocsConnectionImpl invalidate" +toString());
        
        manConImpl.removeAmdocsConnection(this);
    }

    // <editor-fold defaultstate="collapsed" desc=" busines methods ">
    /**
     * Called from client. Calls getAmdocsRef() on this handle's ManagedConnectionImpl.
     *
     * @return      Stub to an Amdocs remote (business) IF.
     * @throws      javax.resource.ResourceException generic exception if operation
     *              fails due to an error condition.
     */
    public Object getAmdocsRef() throws ResourceException {
        logger.log(Level.FINEST, "AmdocsConnectionImpl getAmdocsRef" +toString());
        
        return manConImpl.getAmdocsRef();
    }

    /**
     * Called from client. Clients are able to sign business errors and initiate
     * the removing of the {@link amdocs.ra.outbound.ManagedConnectionImpl ManagedConnection}
     * -belongs to this AmdocsConnection- from the server's connection pool.
     *
     * @throws      javax.resource.ResourceException generic exception if operation
     *              fails due to an error condition.
     */
    public void error(String errorMessage) throws ResourceException {
        logger.log(Level.FINEST, "AmdocsConnectionImpl error" +toString());
        
        manConImpl.error(errorMessage);
    }

    /**
     * Called from client. Closes the client's handle. The physical connection is
     * not affected.
     *
     * @throws      javax.resource.ResourceException generic exception if operation
     *              fails due to an error condition.
     */
    public void close() throws ResourceException {
        /**
         * Closing a connection more than one could confuse server's pool
         */
        synchronized (this) {
            logger.log(Level.FINEST, "AmdocsConnectionImpl close" +toString());
            
            if (isClosed) {
                return;
            }
            isClosed = true;
            /** removes this handle from ManagedConenctionImpl's handle list */
            invalidate();
            /** if this was the last handle of ManagedConenctionImpl then close that too */
            if (manConImpl.getHandleCount() == 0) {
                manConImpl.close(this);
            }
        }
   }
    // </editor-fold>

    /**
     * Provides information about the data in this instance.
     *
     * @return      string representation of the instance's data.
     */
    @Override
    public String toString() {
        return "[creationDate " +creationDate +
            ", creator ManagedConnectionImpl " +
            ((manConImpl == null) ? "null" : manConImpl.toString()) +"]";
    }

    @Override
    public void finalize() {
        logger.log(Level.FINEST, "AmdocsConnectionImpl finalize" +toString());
        try {
            close();
        }
        catch (ResourceException e) {
            logger.log(Level.SEVERE, "AmdocsConnectionImpl finalize" +toString() +
                ", " +e.getMessage());
        }
    }

}
