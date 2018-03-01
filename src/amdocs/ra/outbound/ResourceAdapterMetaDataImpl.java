package amdocs.ra.outbound;

import javax.resource.cci.ResourceAdapterMetaData;

/*
 * ResourceAdapterMetaDataImpl.java
 *
 * Created on 2006. április 4., 11:08
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 * Provides general information about the resource adapter.
 * @author attila.rezner
 */
public class ResourceAdapterMetaDataImpl implements ResourceAdapterMetaData {
    /** Adapter vendor name */
    private static final String	VENDOR_NAME = "Rezner Attila";
    /** 
     * Java connector architecture specification 
     * -which was implemented by this connector- version number 
     */
    private static final String	JCA_SPEC_VERSION = "1.5";
    /** Adapter version number */
    private static final String VERSION = "1.0";
    /** The name of the adapter that was defined by the creator of adapter */
    private static final String ADAPTERNAME = "Amdocs Connection Adapter";
    /** Shosr description of this connector */
    private static final String SHORTDESCRIPTION = "Handles connections to Amdocs' EJBs";
    /** Other supplementary info */
    private static final String[] SPECS = { "" };
        
    /** Creates a new instance of ResourceAdapterMetaDataImpl */
    public ResourceAdapterMetaDataImpl() {
    }

    // <editor-fold defaultstate="collapsed" desc=" inherited from ResourceAdapterMetaData ">
    /**
     * Gets the adapter version number
     * @return adapter version
     */
    public String getAdapterVersion() {
        return VERSION;
    }

    /**
     * Gets the adapter vendor name
     * @return adapter vendor name
     */
    public String getAdapterVendorName() {
        return VENDOR_NAME;
    }

    /**
     * Gets the adapter name defined by its creator
     * @return adapter name
     */
    public String getAdapterName() {
        return ADAPTERNAME;
    }

    /**
     * Gets the adapter short description defined by its creator
     * @return adapter short description
     */
    public String getAdapterShortDescription() {
        return SHORTDESCRIPTION;
    }

    /**
     * Gets the connector architecture specification version number to which this 
     * adapter applies.
     * @return connector architecture specification version number
     */
    public String getSpecVersion() {
        return JCA_SPEC_VERSION;
    }

    /**
     * Gets the supplementary infos defined.
     * @return supplementary info
     */
    public String[] getInteractionSpecsSupported() {
        return SPECS;
    }

    /** 
     * This function is not implemented.
     * @return false -not implemented
     */
    public boolean supportsExecuteWithInputAndOutputRecord() {
        return false;
    }

    /** 
     * This function is not implemented.
     * @return false -not implemented
     */
    public boolean supportsExecuteWithInputRecordOnly() {
        return false;
    }

    /** 
     * This function is not implemented.
     * @return false -not implemented
     */
    public boolean supportsLocalTransactionDemarcation() {
        return false;
    }
    // </editor-fold>
    
}
