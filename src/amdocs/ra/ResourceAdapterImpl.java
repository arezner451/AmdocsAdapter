package amdocs.ra;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;
/*
 * ResourceAdapterImpl.java
 *
 * Created on 2006. április 7., 10:14
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 * This is the main class of the resource adapter. An instance of this class may
 * represent connections to multiple EIS instances. This class is a java bean 
 * which contains configuration information that may be global defaults for 
 * {@link amdocs.ra.outbound.ManagedConnectionFactoryImpl ManagedConnectionFactoryImpl} 
 * instances.
 *
 * @author attila.rezner
 */
public class ResourceAdapterImpl implements ResourceAdapter, Serializable {
    /**
     * JavaBean properties that may define Connection to multiple EIS instances.
     * This ResourceAdapter does not defines any EIS connection parameters, all 
     * connection handling is managed by ManagedConnnectionFactory JavaBean(s).
     */     
    
    /** @see java.util.logging.Logger */
    private static Logger logger = 
        Logger.getLogger("javax.enterprise.resource.resourceadapter");
    
    /**
     * Creates a new instance of ResourceAdapterImpl
     */
    public ResourceAdapterImpl() {
        logger.log(Level.INFO, "ResourceAdapterImpl CTOR");
    }

    // <editor-fold defaultstate="collapsed" desc=" inherited from ResourceAdapter ">    
    /**
     * It is not needed to initialize this ResourceAdapter: not needed to create 
     * resource adapter instance specific objects. BootstrapContext is provided by 
     * AppServer. This makes possible to utilize some AppServer facilities e.g.: 
     * WorkManager.
     * <p>
     * Called by container after ResourceAdapterImpl.CTOR.
     * @param ctx got from container.
     * @throws javax.resource.spi.ResourceAdapterInternalException generic 
     *              exception if operation fails due to an error condition.
     */
    public void start(BootstrapContext ctx) 
    throws ResourceAdapterInternalException {        
        
        logger.log(Level.INFO, "ResourceAdapterImpl start");
    }

    /**
     * It is not needed to prepare for shutdown. 
     * <p>
     * Called during undeploy or server shutdown. Closing network endpoints, 
     * relinquishing threads, releasing all active Work instances, allowing 
     * resource adapter internal in-flight transactions to complete if they are 
     * already in the process of doing a commit, and flushing any cached data to
     * the EIS.
     */
    public void stop() {
        logger.log(Level.INFO, "ResourceAdapterImpl stop");
    }

    /**
     * This ResourceAdapter does not implement inbound connection so empty.
     * <p>
     * AppServer calls this method after start.
     *
     * @param endpointFactory   This serves as a factory for creating message 
     *                          endpoints.
     * @param spec              This interface serves as a marker. An instance 
     *                          of an ActivationSpec must be a JavaBean and must
     *                          be serializable. This holds the activation 
     *                          configuration information for a message endpoint.
     * @throws                  javax.resource.ResourceException generic exception
     *                          if operation fails due to an error condition.
     */
    public void endpointActivation(
        MessageEndpointFactory endpointFactory, ActivationSpec spec) 
    throws ResourceException {
                
        logger.log(Level.INFO, "ResourceAdapterImpl endpointActivation");
    }

    /**
     * This ResourceAdapter does not implement inbound connection so empty.
     * <p>
     * AppServer calls this method before stop.
     *
     * @param endpointFactory   This serves as a factory for creating message 
     *                          endpoints.
     * @param spec              This interface serves as a marker. An instance 
     *                          of an ActivationSpec must be a JavaBean and must
     *                          be serializable. This holds the activation 
     *                          configuration information for a message endpoint.
     */
    public void endpointDeactivation(
        MessageEndpointFactory endpointFactory, ActivationSpec spec) {
                
        logger.log(Level.INFO, "ResourceAdapterImpl endpointDeactivation");        
    }

    /**
     * This method is called by the application server during crash recovery.
     *
     * @param actSpec   an array of ActivationSpec JavaBeans each of which 
     *                  corresponds to an deployed endpoint application that was
     *                  active prior to the system crash.
     * @throws          javax.resource.ResourceException generic exception if 
     *                  operation fails due to an error condition.
     * @return          an array of XAResource objects each of which represents a unique
     *                  resource manager.
     */
    public XAResource[] getXAResources(ActivationSpec[] actSpec) 
    throws ResourceException {
        
        logger.log(Level.INFO, "ResourceAdapterImpl getXAResources");
        
        return null; 
    }
    // </editor-fold>            
    
}
