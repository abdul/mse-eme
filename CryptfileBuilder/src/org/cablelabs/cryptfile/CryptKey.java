
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
