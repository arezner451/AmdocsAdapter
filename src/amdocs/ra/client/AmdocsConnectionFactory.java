package amdocs.ra.client;

import amdocs.ra.outbound.ConnectionSpecImpl;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;

/*
 * AmdocsConnectionFactoryIF.java
 *
 * Created on 2006. május 9., 12:32
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 * This IF can be located in the NamingService of the server that runs the connector.
 * Use this IF to create connections that needed to attach to the Amdocs EJB API.
 *
 * @author attila.rezner
 */
public interface AmdocsConnectionFactory {

    /**
     * Actually unused.
     *
     * @throws      javax.resource.ResourceException On any error.
     * @return      a new connection or an existing connection -if exists already.
     */
    public AmdocsConnection createConnection() throws ResourceException;

    /**
     * Actually call this method to create a connection to Amdocs. The resulting
     * {@link AmdocsConnection AmdocsConnection} can be created only if parameters
     * supplied.
     *
     * @param   amdocsEjbJNDIname   JNDI name of Amdocs EJB intended to use.
     * @param   homeClassName       home class name of Amdocs EJB in JNDI.
     * @param   remoteClassName     remote class name of Amdocs EJB in JNDI.
     * @throws                      javax.resource.ResourceException On any error.
     * @return                      a new or an existing
     *                              {@link AmdocsConnection AmdocsConnection}.
     */
    public AmdocsConnection createConnection(
        String amdocsEjbJNDIname, String homeClassName, String remoteClassName)
    throws ResourceException;

    /**
     * Call this method only in non-managed environment to get reference
     * to ConnecionManager. In this case adapter directly controls pool management
     * providing a class that implements ConnecionManager IF.
     *
     * @return      {@link amdocs.ra.outbound.ConnectionManagerImpl ConnectionManager}.
     */
    public ConnectionManager getConnectionManager();

}
