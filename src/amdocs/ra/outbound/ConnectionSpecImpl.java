package amdocs.ra.outbound;

import javax.resource.cci.ConnectionSpec;
/*
 * ConnectionSpecImpl.java
 *
 * Created on 2006. május 11., 12:55
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 * An instance of this class is a placeholder of the JNDI name of an Amdocs EJB
 * to which client intends to connect. This class also contains the home and remote
 * class names of Amdocs EJB.
 *
 * @author attila.rezner
 */
public class ConnectionSpecImpl implements ConnectionSpec {

    // <editor-fold defaultstate="collapsed" desc=" connection instance specific parameters ">
    /** the name of the EJB in Amdocs's NamingService */
    private String entityName, homeClassName, remoteClassName;
    // </editor-fold>

    /** Creates a new instance of ConnectionSpecImpl */
    public ConnectionSpecImpl() {
    }

    // <editor-fold defaultstate="collapsed" desc=" bean property methods ">
    /**
     * Gets the EJB binding name.
     *
     * @return      the name of the EJB in Amdocs's NamingService
     */
    public String getEntityName() {
        return entityName;
    }

    /**
     * Sets the EJB binding name.
     *
     * @param       entityName      the name of the EJB in Amdocs's NamingService
     */
    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    /**
     * Gets the name of the home class for Amdocs EJB in NamingService
     *
     * @return      the name of the home class for Amdocs EJB in NamingService
     */
    public String getHomeClassName() {
        return homeClassName;
    }

    /**
     * Sets the name of the home class for Amdocs EJB in NamingService
     *
     * @param       homeClassName      the name of the home class for Amdocs EJB in NamingService
     */
    public void setHomeClassName(String homeClassName) {
        this.homeClassName = homeClassName;
    }

    /**
     * Gets the name of the remote class for Amdocs EJB in NamingService
     *
     * @return      the name of the remote class for Amdocs EJB in NamingService
     */
    public String getRemoteClassName() {
        return remoteClassName;
    }

    /**
     * Sets the name of the remote class for Amdocs EJB in NamingService
     *
     * @param      remoteClassName      the name of the remote class for Amdocs EJB in NamingService
     */
    public void setRemoteClassName(String remoteClassName) {
        this.remoteClassName = remoteClassName;
    }
    // </editor-fold>

}
