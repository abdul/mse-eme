// COPYRIGHT_BEGIN
// COPYRIGHT_END

package org.cablelabs.cryptfile;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public interface MP4BoxXML {

    /**
     * Generate the XML representation
     * 
     * @param d the DOM Document
     * @return the XML Element
     */
    public abstract Node generateXML(Document d);

}