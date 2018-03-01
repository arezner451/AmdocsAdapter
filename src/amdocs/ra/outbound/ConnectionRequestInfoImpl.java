package amdocs.ra.outbound;

import javax.resource.spi.ConnectionRequestInfo;
/*
 * ConnectionRequestInfoImpl.java
 *
 * Created on 2006. május 9., 12:50
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 * An instance if this class is used:
 * -during the creation of a new ManagedConnectionImpl.
 * {@link ManagedConnectionFactoryImpl#createManagedConnection ManagedConnectionFactoryImpl.createManagedConnection()},
 * {@link ManagedConnectionFactoryImpl#matchManagedConnections ManagedConnectionFactoryImpl.matchManagedConnections()}
 * -when get a handle to ManagedConnectionImpl
 * {@link ManagedConnectionImpl#getConnection ManagedConnectionImpl.getConnection()}
 *
 * @author attila.rezner
 */
public class ConnectionRequestInfoImpl implements ConnectionRequestInfo {

    // <editor-fold defaultstate="collapsed" desc=" connection instance -ManagedConnectionImpl- specific parameters ">
    /** Amdocs entity name to look for -the name of Amdocs EJB in NamingService */
    private String entityName, homeClassName, remoteClassName;
    // </editor-fold>

    /**
     * Creates a new instance of ConnectionRequestInfoImpl. In this method the
     * name of Amdocs EJB in NamingService is stored in a member variable.
     * @param entityName the name of Amdocs EJB in NamingService
     * @param homeClassName the home class name of Amdocs EJB in NamingService
     * @param remoteClassName the remote class name of Amdocs EJB in NamingService
     */
    public ConnectionRequestInfoImpl(
        String entityName, String homeClassName, String remoteClassName) {

        this.entityName = entityName;
        this.homeClassName = homeClassName;
        this.remoteClassName = remoteClassName;
    }

    // <editor-fold defaultstate="collapsed" desc=" bean property methods ">
    /**
     * Gets the name of Amdocs EJB in NamingService.
     * @return the name of Amdocs EJB in NamingService
     */
    public String getEntityName() {
        return entityName;
    }

    /**
     * Gets the name of the home class name of Amdocs EJB in NamingService.
     * @return the name of the home class name of Amdocs EJB in NamingService
     */
    public String getHomeClassName() {
        return homeClassName;
    }

    /**
     * Gets the name of the remote class name of Amdocs EJB in NamingService.
     * @return the name of the remote class name of Amdocs EJB in NamingService
     */
    public String getRemoteClassName() {
        return remoteClassName;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc=" AppServer connection-pool management ">
    /**
     * This ConnectionRequestInfoImpl instance is equals to obj get as parameter
     * if all its private member valiables are the same than obj's.
     * @param obj the other ConnectionRequestInfoImpl instance to which this instance
     * has to be compared.
     * @return true if this instance's entityName equals with obj's entityName
     */
    public boolean equals(Object obj) {
        ConnectionRequestInfoImpl conReqInfImpl = (ConnectionRequestInfoImpl)obj;

        if (this.entityName.equals(conReqInfImpl.getEntityName())) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Gets the hash value of the name of the Amdocs EJB in the NamingService.
     * @return hash value of the name of the Amdocs EJB in the NamingService
     */
    public int hashCode() {
        return entityName.hashCode();
    }
    // </editor-fold>

    /**
     * Provides information about the data in this instance.
     *
     * @return string representation of the instance's data
     */
    @Override
    public String toString() {
        return "[" +entityName +"]";
    }

}
