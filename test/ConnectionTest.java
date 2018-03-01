import amdocs.csm3g.datatypes.CM9SubInfo;
import amdocs.csm3g.datatypes.PrimaryResourceIdInfo;
import amdocs.csm3g.exceptions.CMException;
import amdocs.csm3g.sessions.interfaces.api.CM9HighLevelAPI;
import amdocs.csm3g.sessions.interfaces.api.SearchServices;
import amdocs.ra.client.AmdocsConnection;
import amdocs.ra.client.AmdocsConnectionFactory;
import amdocs.ra.outbound.ManagedConnectionFactoryImpl;
import java.rmi.RemoteException;
import javax.resource.ResourceException;
import junit.framework.*;
/*
 * ConnectionTest.java
 * JUnit based test
 *
 * Created on 2007. március 1., 10:49
 */

/**
 *
 * @author attila.rezner
 */
public class ConnectionTest extends TestCase {

    private AmdocsConnectionFactory amdocsConnectionFactory;
    
    /**
     * 
     * @param testName 
     */
    public ConnectionTest(String testName) {
        super(testName);
    }

    /**
     * 
     * @throws Exception 
     */
    protected void setUp() throws Exception {
        ManagedConnectionFactoryImpl managedConnectionFactoryImpl =
            new ManagedConnectionFactoryImpl();
        
        // set EIS properties                
//        managedConnectionFactoryImpl.setHost("phx013");
//        managedConnectionFactoryImpl.setPort(15475);
//        managedConnectionFactoryImpl.setEnvironment("vfh32_LE");
//        managedConnectionFactoryImpl.setUserName("vfh32");
//        managedConnectionFactoryImpl.setPassword("Unix11");

        managedConnectionFactoryImpl.setHost("phx013");
        managedConnectionFactoryImpl.setPort(15435);
        managedConnectionFactoryImpl.setEnvironment("vfh35_LE");
        managedConnectionFactoryImpl.setUserName("vfh35");
        managedConnectionFactoryImpl.setPassword("Unix11");
        
        amdocsConnectionFactory = (AmdocsConnectionFactory)
            managedConnectionFactoryImpl.createConnectionFactory();            
    }

    /**
     * 
     * @throws Exception 
     */
    protected void tearDown() throws Exception {
    }
    
    // TODO add test methods here. The name must begin with 'test'. For example:
    // public void testHello() {}
    /**
     * 
     * @throws ResourceException 
     */
    public void testConnection() throws ResourceException {
                      
        AmdocsConnection amdocsConnection =
            amdocsConnectionFactory.createConnection("amdocsBeans/CM1SearchServicesHome", "amdocs.csm3g.sessions.interfaces.home.SearchServicesHome", "amdocs.csm3g.sessions.interfaces.api.SearchServices");
        
        SearchServices searchServices =
            (SearchServices)amdocsConnection.getAmdocsRef();        

//        assertNotNull(searchServices);
        
        amdocsConnection.close();             
/*        
        AmdocsConnection amdocsConnection =
            amdocsConnectionFactory.createConnection("amdocsBeans/CM9HighLevelAPIHome");
        
        CM9HighLevelAPI cM9HighLevelAPI =
            (CM9HighLevelAPI)amdocsConnection.getAmdocsRef();        

        assertNotNull(cM9HighLevelAPI);
        
        amdocsConnection.close();                
*/
    }

    /**
     * 
     * @throws ResourceException
     * @throws CMException
     * @throws RemoteException 
     */
    public void _testGetCustData() throws ResourceException, CMException, RemoteException {
                    
        AmdocsConnection amdocsConnection =
            amdocsConnectionFactory.createConnection("amdocsBeans/CM9HighLevelAPIHome", "amdocs.csm3g.sessions.interfaces.home.CM9HighLevelAPIHome" ,"amdocs.csm3g.sessions.interfaces.api.CM9HighLevelAPI");

        CM9HighLevelAPI cM9HighLevelAPI =
            (CM9HighLevelAPI)amdocsConnection.getAmdocsRef();        

        assertNotNull(cM9HighLevelAPI);

        PrimaryResourceIdInfo resInf = new PrimaryResourceIdInfo();
        resInf.setPrimResourceTp("C");
        
        CM9SubInfo subsInfo = cM9HighLevelAPI.l9getsubscriberInfo(resInf);
        System.out.println(subsInfo.toString());
        
        amdocsConnection.close();        
    }
    
}
