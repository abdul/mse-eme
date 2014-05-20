
package org.cablelabs.cryptfile;

import org.apache.commons.codec.binary.Hex;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Defines an encyption key as used in the MP4Box crypt files
 */
public class CryptKey implements MP4BoxXML {
    
    private byte[] keyID;
    private byte[] key;
    
    private static final String ELEMENT = "key";
    private static final String ATTR_KEYID = "KID";
    private static final String ATTR_KEY = "value";
    
    /**
     * Create a new encryption key specification
     * 
     * @param keyID
     * @param key
     */
    public CryptKey(byte[] keyID, byte[] key) {
        
        if (keyID == null || keyID.length != 16)
            throw new IllegalArgumentException("Invalid KeyID: " + keyID);
        if (key == null || key.length != 16)
            throw new IllegalArgumentException("Invalid Key: " + key);
        
        this.keyID = keyID;
        this.key = key;
    }

    /**
     * Returns the 16-byte Key ID
     * 
     * @return the keyID
     */
    public byte[] getKeyID() {
        return keyID;
    }
    
    /**
     * Returns the 16-byte AES encryption key
     * 
     * @return the key
     */
    public byte[] getKey() {
        return key;
    }

    /*
     * (non-Javadoc)
     * @see org.cablelabs.cryptfile.MP4BoxXML#generateXML(org.w3c.dom.Document)
     */
    @Override
    public Node generateXML(Document d) {
        
        Element e = d.createElement(ELEMENT);
        e.setAttribute(ATTR_KEYID, "0x" + Hex.encodeHexString(keyID));
        e.setAttribute(ATTR_KEY, "0x" + Hex.encodeHexString(key));
        return e;
    }
}
