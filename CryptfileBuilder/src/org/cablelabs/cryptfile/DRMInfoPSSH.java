/* Copyright (c) 2014, CableLabs, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

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
    
    /**
     * Generates the base DRMInfo element with a system ID child element
     * 
     * @param d the DOM Document
     * @return the element
     */
    protected Element generateDRMInfo(Document d) {
       Element e = d.createElement(ELEMENT);
       e.setAttribute(ATTR_TYPE, "pssh");
       e.setAttribute(ATTR_VERSION, "0");
       
       Bitstream b = new Bitstream();
       b.setupID128(systemID);
       
       e.appendChild(b.generateXML(d));
       
       return e;
    }
}
