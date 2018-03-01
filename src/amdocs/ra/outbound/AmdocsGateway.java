package amdocs.ra.outbound;

import amdocs.uams.UamsPasswordCredential;
import amdocs.uams.UamsSystem;
import amdocs.uams.login.UamsLoginContext;
import amdocs.uams.login.direct.DirectLoginService;
import amdocs.uamsimpl.shared.login.EnvironmentAdditionalData;
import amdocs.uamsx.acm.shared.obj.ACMConstants;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.Date;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.resource.ResourceException;

/*
 * Amdocs.java
 *
 * Created on 2006. május 11., 17:09
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 * If Amdocs login not successfull when resource adapter starts then RuntimeException
 * is raised when calling uamsLoginCtx.getTicket() in createAmdCtxRefreshTimer.
 * So if RuntimeException is not explicitly catched in createAmdCtxRefreshTimer
 * then no timer thread starts (timer thread start is the next step in CTOR
 * after this method -setAPI).
 * @author attila.rezner
 */
public class AmdocsGateway {
    /** @see java.util.logging.Logger */
    private static Logger logger = 
        Logger.getLogger("javax.enterprise.resource.resourceadapter");
    /** Class that provides the context into NamingService on Amdocs's side. */
    private static String contextFactory = "weblogic.jndi.WLInitialContextFactory";
    /** All instances of AmdocsGateway shares this ticket */
    private static String ticket;    
    /** {@link ManagedConnectionImpl ManagedConnectionImpl} */
    private ManagedConnectionImpl manConImpl;
    /** {@link amdocs.ra.share.ConnectionSpecImpl ConnectionSpecImpl} */
    private ConnectionSpecImpl conSpecImpl;
    /** A Stub to a specific Amdocs EJB remote IF */
    private Object entityRef;
    /** creation date of this AmdocsGateway */
    private Date creationDate,
    /** date of last invocation of getAPI of this AmdocsGateway */            
                 lastUsageDate;

    private Hashtable<String,String> namingEnvironment;
    private Method createMethod, removeMethod;
    
    private DirectLoginService directLoginSvc;

    /**  
     * Set ticket -shared by all physical connection-. Ticket is set only during 
     * the creation of the 1st physical connection to Amdocs.
     *
     * @param _ticket the value to set the ticket to.
     */
    public synchronized static void setTicket(String _ticket) {
        logger.log(Level.FINEST, "AmdocsGateway setTicket from " +
            (ticket != null ? ticket : "no ticket") +" to " +_ticket);
        
        ticket = _ticket;
    }
    
    /**
     * Get ticket -shared by all physical connection-. Ticket re-news only if
     * all physical connections destroyed before and a new connection request
     * received.
     * 
     * @return      actual ticket data.
     */
    public synchronized static String getTicket() {
        logger.log(Level.FINEST, "AmdocsGateway getTicket " +ticket);
                
        return ticket;
    }
    
    /**
     * In case of any exception (means that no Amdocs connection can be established),
     * {@link ManagedConnectionImpl ManagedConnectionImpl} to wich this
     * AmdocsGateway belongs will be destroyed.
     * <p>
     * Called from AmdocsGateway.CTOR.
     *
     * @param       info        information about the caller of this method can be
     *                          passed in this parameter (serves debug).
     */
    private static void setLoginContext(String environment, String userName, String password) 
    throws ResourceException {
        
        logger.log(Level.FINEST, "AmdocsGateway setLoginContext env " +
            environment +", user " +userName +", passw " +password);
        
        try {
            String accessHost = InetAddress.getLocalHost().getHostName();
            
            Hashtable<String,String> secTicketData = new Hashtable<String,String>(2);
            secTicketData.put(ACMConstants.APPLICATION_ID_KEY, "CM");
            secTicketData.put(
                EnvironmentAdditionalData.SELECTED_USER_ENV_KEY, environment);
            
            DirectLoginService directLoginSvc = (DirectLoginService)UamsSystem.getService(
                null, UamsSystem.LN_UAMS_DIRECT_LOGIN);
                        
            UamsLoginContext uamsLoginCtx = directLoginSvc.login(
                null, userName, new UamsPasswordCredential(password), 
                null, 0L, accessHost, accessHost, secTicketData);

            switch (uamsLoginCtx.getStatus()) {
                case UamsLoginContext.COMPLETE:
                    setTicket(uamsLoginCtx.getTicket());
                    break;
                case UamsLoginContext.FAILURE:
                    logger.log(Level.SEVERE, "EIS login Failure");
                    throw new ResourceException("EIS login Failure");
                case UamsLoginContext.REJECTED:
                    logger.log(Level.SEVERE, "EIS login Rejected");
                    throw new ResourceException("EIS login Rejected");
                default:
                    logger.log(Level.SEVERE, "EIS error during login");
                    throw new ResourceException("EIS error during login");
            }
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "AmdocsGateway setLoginContext " +e.getMessage());
            throw new ResourceException(e.getMessage());
        }
    }
    
    /**
     * Called when no physical connection left in the pool.
     * <p>
     * Called from {@link ManagedConnectionFactoryImpl#getInvalidConnections ManagedConnectionFactory.getInvalidConnections}
     */
    public synchronized static void releaseLoginContext() {
        // if ticket is set
        if (getTicket() != null && !getTicket().equals("")) {
            // log
            logger.log(Level.FINEST, "AmdocsGateway releaseLoginContext");
            
            try {
                DirectLoginService directLoginSvc = (DirectLoginService)
                    UamsSystem.getService(null, UamsSystem.LN_UAMS_DIRECT_LOGIN);
                
                directLoginSvc.logout(getTicket());
            }
            catch (Exception e) {
                logger.log(Level.SEVERE, "AmdocsGateway releaseLoginContext " +
                    e.getMessage());
            }
            finally {
                setTicket("");
            }
        }
    }
    
    /**
     * Creates a new instance of AmdocsGateway. NamingException can be raised in
     * setAPI() when no entity found in JNDI with the supplied name.
     * <p>
     * Called from {@link ManagedConnectionImpl ManagedConnectionImpl.CTOR}.
     *
     * @param       manConImpl      {@link ManagedConnectionImpl ManagedConnectionImpl}
     *                              to which this instance will be a handle.
     * @param       conSpecImpl     {@link ConnectionSpecImpl ConnectionSpecImpl}.
     */
    public AmdocsGateway (
        ManagedConnectionImpl manConImpl, ConnectionSpecImpl conSpecImpl) {

        creationDate = new Date();

        logger.log(Level.INFO, "AmdocsGateway CTOR[" +
            "ManagedConnectionFactoryImpl" +manConImpl.getManConFactImpl().toString() +
            ", ConnectionSpecImpl " +conSpecImpl.getEntityName() +"]");

        this.manConImpl = manConImpl;
        this.conSpecImpl = conSpecImpl;

        namingEnvironment = new Hashtable<String,String>();
	namingEnvironment.put(Context.INITIAL_CONTEXT_FACTORY, contextFactory);
        namingEnvironment.put(Context.PROVIDER_URL,
            "t3://" +manConImpl.getManConFactImpl().getHost() +
            ":" +manConImpl.getManConFactImpl().getPort());
        
        try {
            Class beanHomeClass = Class.forName(conSpecImpl.getHomeClassName());
            createMethod = beanHomeClass.getMethod("create", new Class[0]);//null

            Class beanRemoteClass = Class.forName(conSpecImpl.getRemoteClassName());
            removeMethod = beanRemoteClass.getMethod("remove", new Class[0]);

            directLoginSvc = (DirectLoginService)UamsSystem.getService(
                null, UamsSystem.LN_UAMS_DIRECT_LOGIN);

            // these tasks executed only if no error raised previously -begin
            synchronized (this.getClass()) {
                if (getTicket() == null || getTicket().equals("")) {
                    setLoginContext(
                        manConImpl.getManConFactImpl().getEnvironment(),
                        manConImpl.getManConFactImpl().getUserName(),
                        manConImpl.getManConFactImpl().getPassword()
                    );
                }                
            }                                                
            setAPI(" from AmdocsGateway CTOR ");
            // these tasks executed only if no error raised previously -end
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "AmdocsGateway CTOR " +e.getMessage());
            // notify ManagedConnectionImpl's eventListener about error
            manConImpl.error("AmdocsGateway CTOR " +e.getMessage());
        }
    }
        
    /**
     * In case of any exception (means that no Amdocs connection can be established),
     * {@link ManagedConnectionImplManagedConnectionImpl} to wich this AmdocsGateway
     * belongs will be destroyed.
     * <p>
     * Called from AmdocsGateway.CTOR. 
     *
     * @param       info        information about the caller of this method can be
     *                          passed in this parameter (serves debug).
     */
    private void setAPI(String info) {
        // log
        logger.log(Level.FINEST, "AmdocsGateway setAPI " +info +", ticket " +getTicket());
        
        try {
            /**
             * if Amdocs login not successfull when resource adapter starts then
             * RuntimeException is raised when calling uamsLoginCtx.getTicket().
             * So if RuntimeException is not explicitly catched here then no timer
             * thread starts (timer thread start is the next step in CTOR after
             * this method).
             */            
            namingEnvironment.put(Context.SECURITY_PRINCIPAL, getTicket());

            InitialContext ctx = new InitialContext(namingEnvironment);
            // entityRef contains remoteHomeIF
            entityRef = ctx.lookup(conSpecImpl.getEntityName());
            // entityRef now contains remoteIF so business calls can invoked on adapter
            entityRef = createMethod.invoke(entityRef, new Object[0]);//null
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "AmdocsGateway setAPI " +info +e.getMessage());
            // invalidate amdocs EJB reference
            entityRef = null;
            setTicket("");
            // notify ManagedConnectionImpl's eventListener about error
            manConImpl.error("AmdocsGateway setAPI " +info +e.getMessage());
        }
    }

    /**
     * Gets Amdocs EJB's remote IF.
     * <p>
     * Called from 
     * {@link ManagedConnectionFactoryImpl#getInvalidConnections ManagedConnectionFactoryImpl.getInvalidConnections},
     * {@link ManagedConnectionImpl#getAmdocsRef ManagedConnectionImpl.getAmdocsRef}, 
     * {@link ManagedConnectionImpl#toString ManagedConnImpl.toString}.

     * @param       info        information about the caller of this method can be
     *                          passed in this parameter (serves debug).
     * @return      reference to business interface of the Amdocs EJB.
     */
    public Object getAPI(String info) {
        // log
        logger.log(Level.FINEST, "AmdocsGateway getAPI " +info +toString());
        
        lastUsageDate = new Date(System.currentTimeMillis());
        // remoteBeanIF
        return entityRef;
    }

    /**
     * Remove remoteBeanIF -only possible if entityRef is not null.
     * <p>
     * Called from {@link ManagedConnectionImpl#destroy ManagedConnectionImpl.destroy}.
     * 
     * @param info API name to release.
     * 
     */
    public void releaseAPI(String info) {
        // log
        logger.log(Level.FINEST, "AmdocsGateway releaseAPI " +info +toString());
        // if this handle has a valid Amdocs remote IF reference
        if (entityRef != null) {
            try {
                // entityRef contains remoteIF
                removeMethod.invoke(entityRef, new Object[0]);
            }
            catch (Exception e) {
                logger.log(Level.SEVERE, "AmdocsGateway releaseAPI " +info +
                    toString() +", " +e.getMessage());
            }
            finally {
                // remoteBeanIF
                entityRef = null;
            }
        }
    }

    /**
     * Provides information about the data in this instance.
     *
     * @return      string representation of the instance's data.
     */
    @Override
    public String toString() {
        return "[creationDate " +creationDate +
            ", lastUsageDate " +(lastUsageDate != null ? lastUsageDate : "not used")+
            ", ManagedConnectionFactoryImpl " +manConImpl.getManConFactImpl().toString() +
            ", ConnectionSpecImpl " +conSpecImpl.getEntityName() +
            ", ticket " +getTicket() +
            ", entityRef " +(entityRef == null ? "null" : "valid]");
    }

    /**
     * Gets the last usage date of this AmdocsGateway instance.
     * <p>
     * Called from {@link ManagedConnectionFactoryImpl#getInvalidConnections ManagedConnectionFactoryImpl.getInvalidConnections}.
     *
     * @return      date of last usage of this AmdocsGateway instance.
     */
    public Date getLastUsageDate() {
        return lastUsageDate;
    }
            
}
