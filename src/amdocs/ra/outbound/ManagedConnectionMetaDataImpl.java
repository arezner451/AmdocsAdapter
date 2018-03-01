package amdocs.ra.outbound;

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnectionMetaData;
/*
 * ManagedConnectionMetaDataImpl.java
 *
 * Created on 2006. április 4., 11:08
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 * ManagedConnectionMetaData provides information about the underlying EIS 
 * instance associated with a ManagedConnection instance
 * Used in ManagedConnectionImpl.getMetaData()
 *
 * @author attila.rezner
 */
public class ManagedConnectionMetaDataImpl implements ManagedConnectionMetaData {
    /** The name of EIS to which this connector connects */
    private String eisProductName = "Amdocs";
    /** The EIS product version to which this connector connects */
    private String eisProductVersion = "Test system";
    /** 
     * Maximum allowed ManagedConnectionImpls. This is the number of Amdocs EJBs
     * that can be reached paralel.
     */
    private int maxConnections = 10000;
    /** userName that used to connect to EIS */
    private String userName;
    
    /** Creates a new instance of ManagedConnectionMetaDataImpl */
    public ManagedConnectionMetaDataImpl() {
    }

    /**
     * Creates a new ManagedConnectionMetaDataImpl. Called in 
     * ManagedConnectionImpl.getMetaData
     * @param manConFactImpl ManagedConnectionFactoryImpl that created the ManagedConnectionImpl
     */
    public ManagedConnectionMetaDataImpl(ManagedConnectionFactoryImpl manConFactImpl) {
        userName = manConFactImpl.getUserName();
    }

    // <editor-fold defaultstate="collapsed" desc=" inherited from ManagedConnectionMetaData ">
    /**
     * Gets the name of EIS to which this connector connects
     * @return name of EIS
     * @throws javax.resource.ResourceException generic exception if operation fails due to an error condition
     */
    public String getEISProductName() throws ResourceException {
        return eisProductName;
    }

    /**
     * Gets the EIS product version to which this connector connects
     * @return EIS product version
     * @throws javax.resource.ResourceException generic exception if operation fails due to an error condition
     */
    public String getEISProductVersion() throws ResourceException {
        return eisProductVersion;
    }

    /**
     * Gets the maximum allowed ManagedConnectionImpls. This is the number of Amdocs 
     * EJBs that can be reached paralel.
     * @return maximum allowed ManagedConnectionImpls to EIS
     * @throws javax.resource.ResourceException generic exception if operation fails due to an error condition
     */
    public int getMaxConnections() throws ResourceException {
        return maxConnections;
    }

    /**
     * Gets the userName that used to connect to EIS.
     * @return userName that used to connect to EIS
     * @throws javax.resource.ResourceException generic exception if operation fails due to an error condition
     */
    public String getUserName() throws ResourceException {
        return userName;
    }
    // </editor-fold>
    
}
