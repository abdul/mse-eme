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

package org.cablelabs.playready;

import java.io.ByteArrayOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Generates a PlayReady Windows Rights Management Header XML document
 */
public class WRMHeader {
    
    public enum Version {
        V_4000("4.0.0.0"),
        V_4100("4.1.0.0");
        
        private String xml_attr;
        
        Version(String xml_attr) {
            this.xml_attr = xml_attr;
        }
        
        String toXMLAttr() {
            return xml_attr;
        }
    }
    
    private static final String XMLNS = "http://schemas.microsoft.com/DRM/2007/03/PlayReadyHeader";
    
    private Document doc;
    
    // Build a version 4.0.0.0 WRM Header
    private void build4000Doc(PlayReadyKeyPair key, String url, Element root) {
        Element data = doc.createElement("DATA");
        root.appendChild(data);
        
        Element protectinfo = doc.createElement("PROTECTINFO");
        data.appendChild(protectinfo);
        
        Element keylen = doc.createElement("KEYLEN");
        keylen.setTextContent("16");
        Element algid = doc.createElement("ALGID");
        algid.setTextContent("AESCTR");
        protectinfo.appendChild(keylen);
        protectinfo.appendChild(algid);
        
        Element kid = doc.createElement("KID");
        kid.setTextContent(Base64.encodeBase64String(key.getMSKeyID()));
        Element checksum = doc.createElement("CHECKSUM");
        checksum.setTextContent(Base64.encodeBase64String(key.getChecksum()));
        Element la_url = doc.createElement("LA_URL");
        la_url.setTextContent(url);
        data.appendChild(kid);
        data.appendChild(checksum);
        data.appendChild(la_url);
    }
    
    // Build a version 4.1.0.0 WRM Header
    private void build4100Doc(PlayReadyKeyPair key, String url, Element root) {
        // This is not working in my scripts yet, so I'm not going to implement it
        // here until I have a working test
    }
    
    public WRMHeader(Version version, PlayReadyKeyPair key, String url) {
        
        // Create a new document
        DocumentBuilder builder = null;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        }
        catch (ParserConfigurationException ex) {
            System.out.println("Error creating XML DocumentBuilder: " + ex.getMessage());
            System.exit(1);
        }
        doc = builder.newDocument();
        
        // Create root element and set namespace and version
        Element root = doc.createElement("WRMHEADER");
        root.setAttribute("xmlns", XMLNS);
        root.setAttribute("version", version.toXMLAttr());
        doc.appendChild(root);
        
        // Build the version-specific document
        switch (version) {
        case V_4000:
            build4000Doc(key, url, root);
            break;
        case V_4100:
            build4100Doc(key, url, root);
            break;
        default:
            break;
        }
    }
    
    /**
     * Returns the WRMHeader data exactly as it should be used in the PlayReady PSSH 
     * 
     * @return the WRMHeader data
     */
    public byte[] getWRMHeaderData() {
        
        // Setup our transformer to output UTF16 little-endian, with no declaration and no
        // indenting
        Transformer tf = null;
        try {
            tf = TransformerFactory.newInstance().newTransformer();
            tf.setOutputProperty(OutputKeys.ENCODING, "UTF-16LE");
            tf.setOutputProperty(OutputKeys.INDENT, "no");
            tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        }
        catch (Exception ex) {
            System.out.println("Error creating XML Transformer: " + ex.getMessage());
            System.exit(1);;
        }
        
        byte[] retVal = null;
        
        // Write the document to a byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(baos);
        try {
            tf.transform(source, result);
            retVal = baos.toByteArray();
        }
        catch (TransformerException ex) {
            System.out.println("Error performing XML transform: " + ex.getMessage());
        }
        
        return retVal;
    }

}
