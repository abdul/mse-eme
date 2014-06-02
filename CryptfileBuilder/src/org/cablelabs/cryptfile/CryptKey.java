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

import org.apache.commons.codec.binary.Hex;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Defines an encyption key as used in the MP4Box crypt files
 */
public class CryptKey implements MP4BoxXML {
    
    private KeyPair keypair;
    
    private static final String ELEMENT = "key";
    private static final String ATTR_KEYID = "KID";
    private static final String ATTR_KEY = "value";
    
    /**
     * Create a new encryption key specification
     * 
     * @param key the keyID/key pair
     */
    public CryptKey(KeyPair keypair) {
        
        if (keypair == null)
            throw new IllegalArgumentException("Invalid Key: " + keypair);
        
        this.keypair = keypair;
    }

    /**
     * Returns the KeyPair associated with this CryptKey
     * 
     * @return the key pair
     */
    public KeyPair getKeyPair() {
        return keypair;
    }
    
    /*
     * (non-Javadoc)
     * @see org.cablelabs.cryptfile.MP4BoxXML#generateXML(org.w3c.dom.Document)
     */
    @Override
    public Node generateXML(Document d) {
        
        Element e = d.createElement(ELEMENT);
        e.setAttribute(ATTR_KEYID, "0x" + Hex.encodeHexString(keypair.getID()));
        e.setAttribute(ATTR_KEY, "0x" + Hex.encodeHexString(keypair.getKey()));
        return e;
    }
}
