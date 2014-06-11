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

package org.cablelabs.clearkey.cryptfile;

import java.net.URL;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.cablelabs.cryptfile.Bitstream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Generates CableLabs ClearKey "remote" PSSH for MP4Box cryptfiles.
 * This variant of the PSSH instructs the player to retrieve keys from
 * a remote server using a specific URL
 */
public class ClearKeyRemotePSSH extends ClearKeyPSSH {
    
    private URL url;
    private List<String> keyIDs;

    /**
     * Create a "remote" PSSH.  This PSSH instructs the player to retrieve
     * the keys from a remote server using the given URL
     * 
     * @param url the clearkey server url 
     * @param keyIDs a list of key IDs for keys that will be retrieved from
     * the server
     */
    public ClearKeyRemotePSSH(URL url, List<String> keyIDs) {
        super();
        this.url = url;
        this.keyIDs = keyIDs;
    }
    
    /*
     * (non-Javadoc)
     * @see org.cablelabs.cryptfile.MP4BoxXML#generateXML(org.w3c.dom.Document)
     */
    @Override
    public Node generateXML(Document d) {
        Element e = generateDRMInfo(d);
        Bitstream b = new Bitstream();
        
        // ClearKey Type = 1 for JSON
        b.setupInteger(1, 8);
        e.appendChild(b.generateXML(d));
        
        String urlString = url.toString();
        if (url.getPath() == null) {
            urlString += "/";
        }
        
        // Append query string
        urlString += "?";
        for (String keyID : keyIDs) {
            urlString += "keyid=" + keyID.replaceAll("/-/", "") + "&";
        }
        urlString = urlString.substring(0, urlString.length()-1);
        
        b.setupString(Base64.encodeBase64String(urlString.getBytes()), 16);
        e.appendChild(b.generateXML(d));
        
        return e;
    }
}
