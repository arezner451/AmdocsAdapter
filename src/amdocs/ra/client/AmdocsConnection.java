package amdocs.ra.client;

import javax.resource.ResourceException;
/*
 * AmdocsConnectionIF.java
 *
 * Created on 2006. május 9., 11:57
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 * Adapter client applications use this IF to attach to a specific Amdocs EJB API.
 *
 * @author attila.rezner
 */
public interface AmdocsConnection {

    /**
     * Gets a reference to a specific Amdocs EJB remote (business) IF. After a
     * cast to the proper EJB remote IF class, business methods can be called.
     *
     * @return      Stub to an Amdocs remote (business) IF.
     * @throws      javax.resource.ResourceException on any error.
     */
    public Object getAmdocsRef() throws ResourceException;

    /**
     * Clients are able to sign business errors and initiate the removing of the
     * {@link amdocs.ra.outbound.ManagedConnectionImpl ManagedConnection} -belongs
     * to this AmdocsConnection- from the server's connection pool.
     *
     * @param errorMessage business error message to show.
     * @throws      javax.resource.ResourceException generic exception if operation
     *              fails due to an error condition.
     */
    public void error(String errorMessage) throws ResourceException;

    /**
     * Call this method from client to close session.
     *
     * @throws      javax.resource.ResourceException on any error.
     */
    public void close() throws ResourceException;

}
