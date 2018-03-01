package amdocs.ra.outbound;

import javax.resource.ResourceException;
import javax.resource.cci.ConnectionMetaData;
/*
 * ConnectionMetaDataImpl.java
 *
 * Created on 2006. április 13., 10:54
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 * ConnectionMetaData provides information about an EIS instance connected 
 * through a Connection instance
 * Used in AmdocsConnectionImpl.getMetaData()
 *
 * @author attila.rezner
 */
public class ConnectionMetaDataImpl implements ConnectionMetaData {
    /** ManagedConnectionImpl to which this instance stores connection data */
    private ManagedConnectionImpl manConImpl;
        
    /** Creates a new instance of ConnectionMetaDataImpl */
    public ConnectionMetaDataImpl() {
    }

    /**
     * Creates a new instance of ConnectionMetaDataImpl. ManagedConnectionImpl -to 
     * which this instance stores connection data- is stored in a member variable.
     * @param mci ManagedConnectionImpl to which this instance stores connection data
     */
    public ConnectionMetaDataImpl(ManagedConnectionImpl mci) {
        manConImpl = mci;
    }

    /**
     * Gets the name of EIS to which this connector connects. This information
     * is from ManagedConnectionImpl's ManagedConnectionMetaDataImpl instance.
     * @return name of EIS
     * @throws javax.resource.ResourceException generic exception if operation fails due to an error condition
     */
    public String getEISProductName() throws ResourceException {
        return manConImpl.getMetaData().getEISProductName();
    }

    /**
     * Gets the EIS product version to which this connector connects. This information
     * is from ManagedConnectionImpl's ManagedConnectionMetaDataImpl instance.
     * Gets the EIS product version to which this connector connects
     * @return EIS product version
     * @throws javax.resource.ResourceException generic exception if operation fails due to an error condition
     */
    public String getEISProductVersion() throws ResourceException {
        return manConImpl.getMetaData().getEISProductVersion();
    }

    /**
     * Gets the userName that used to connect to EIS. This information is from 
     * ManagedConnectionImpl's ManagedConnectionMetaDataImpl instance.
     * @return userName that used to connect to EIS
     * @throws javax.resource.ResourceException generic exception if operation fails due to an error condition
     */
    public String getUserName() throws ResourceException {
        return manConImpl.getMetaData().getUserName();
    }
    
}
