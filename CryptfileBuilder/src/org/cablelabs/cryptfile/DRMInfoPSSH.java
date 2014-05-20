
package org.cablelabs.cryptfile;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Base class for creating DRM-specific <i>DRMInfo</i> elements for use in the 
 * MP4Box cryptfile
 */
public abstract class DRMInfoPSSH implements MP4BoxXML {
    
    private static final String ELEMENT = "DRMInfo";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_VERSION = "version";
    
    protected byte[] systemID; 
    
    /**
     * Construct a new DRMInfo element
     * 
     * @param systemID the unique identifier registered to a particular DRM system
     */
    protected DRMInfoPSSH(byte[] systemID) {
        
        if (systemID.length != 16)
            throw new IllegalArgumentException("Invalid PSSH system ID: length = " + systemID.length);
        
        this.systemID = systemID;
    }
    
    protected Element generateDRMInfo(Document d) {
       Element e = d.createElement(ELEMENT);
       e.setAttribute(ATTR_TYPE, "pssh");
       e.setAttribute(ATTR_VERSION, "0");
       return e;
    }
}
