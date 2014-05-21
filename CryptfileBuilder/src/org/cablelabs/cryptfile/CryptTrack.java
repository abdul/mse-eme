
package org.cablelabs.cryptfile;

import java.util.ArrayList;
import java.util.List;
import java.security.SecureRandom;

import org.apache.commons.codec.binary.Hex;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Defines a single track within an ISOBMFF file which may or may not
 * be specified for encryption
 */
public class CryptTrack implements MP4BoxXML {
    
    private int trackID;
    private boolean isEncrypted = false;
    private int ivSize = 0;
    private byte[] iv;
    
    private List<CryptKey> keys;
    private int keyRoll = -1;
    
    private static final String ELEMENT = "CrypTrack";
    private static final String ATTR_TRACK_ID = "trackID";
    private static final String ATTR_IS_ENCRYPTED = "isEncrypted";
    private static final String ATTR_IV_SIZE = "IV_size";
    private static final String ATTR_FIRST_IV = "first_IV";
    private static final String ATTR_SAI_BOX = "saiSavedBox";
    private static final String ATTR_KEY_ROLL = "keyRoll";
    
    /**
     * 8-byte (64-bit) initialization vector
     */
    public static final int IV_SIZE_8  = 8;
    
    /**
     * 16-byte (128-bit) initialization vector
     */
    public static final int IV_SIZE_16 = 16;
    
    // Private constructor shared by public ones
    private CryptTrack(int trackID, int ivSize, byte[] iv) {
        if (trackID < 0)
            throw new IllegalArgumentException("Invalid track ID: " + trackID);
        if (ivSize != IV_SIZE_8 && ivSize != IV_SIZE_16)
            throw new IllegalArgumentException("Invalid initialization vector size : " + ivSize);
        if (iv != null && iv.length != ivSize)
            throw new IllegalArgumentException("Invalid initialization vector : size = " + iv.length);
        
        this.trackID = trackID;
        this.isEncrypted = true;
        this.ivSize = ivSize;
        
        // Generate random IV or use the one provided
        if (iv == null) {
            this.iv = new byte[ivSize];
            SecureRandom sr = new SecureRandom();
            sr.nextBytes(this.iv);
        }
        else {
            this.iv = iv;
        }
        
    }

    /**
     * Create a non-encrypted track
     * 
     * @param trackID the track ID found in the ISOBMFF track header
     * @param ivSize the length of the initialization vector (either IV_SIZE_8 or
     * IV_SIZE_16)
     */
    public CryptTrack(int trackID, int ivSize) {
        this.trackID = trackID;
    }
    
    /**
     * Create a new track encrypted with a single key
     * 
     * @param trackID the track ID found in the ISOBMFF track header
     * @param ivSize the length of the initialization vector (either IV_SIZE_8 or
     * IV_SIZE_16)
     * @param iv the initialization vector, or null if you want a random IV of the given
     * IV length generated for you
     * @param key the key that will encrypt the track
     */
    public CryptTrack(int trackID, int ivSize, byte[] iv, CryptKey key) {
        this(trackID, ivSize, iv);
        
        if (key == null)
            throw new IllegalArgumentException("CryptKey may not be null");
        
        keys = new ArrayList<CryptKey>(1);
        keys.add(key);
    }
    
    /**
     * Create a new track encrypted with rolling keys.  Each N samples (when N is specified
     * by the <i>keyRoll</i> parameter) will be encrypted with the same key.  After the last
     * key in the list is used to encrypt a set of N samples, the first key in the list will
     * be used to encrypt the next set of samples
     * 
     * @param trackID the track ID found in the ISOBMFF track header
     * @param ivSize the length of the initialization vector (either IV_SIZE_8 or
     * IV_SIZE_16)
     * @param iv the initialization vector, or null if you want a random IV of the given
     * IV length generated for you
     * @param key the keys that will encrypt the track
     * @param keyRoll the number of consecutive samples that will be encrypted with a
     * particular key.  If key list contains only one key, this parameter is ignored
     */
    public CryptTrack(int trackID, int ivSize, byte[] iv, List<CryptKey> keys, int keyRoll) {
        this(trackID, ivSize, iv);
        
        if (keys == null || keys.isEmpty())
            throw new IllegalArgumentException("CryptKey list may not be null or empty");
        if (keys.size() > 1 && keyRoll < 1)
            throw new IllegalArgumentException("KeyRoll value must be greater than 0 when multiple keys are specified");
        
        this.keys = new ArrayList<CryptKey>(keys);
        if (keys.size() > 1)
            this.keyRoll = keyRoll;
    }
    
    /**
     * Add a new encryption key to this track
     * 
     * @param key the key
     */
    public void addKey(CryptKey key) {
        keys.add(key);
    }

    /* (non-Javadoc)
     * @see org.cablelabs.cryptfile.MP4BoxXML#generateXML(org.w3c.dom.Document)
     */
    @Override
    public Node generateXML(Document d) {
        Element e = d.createElement(ELEMENT);
        e.setAttribute(ATTR_TRACK_ID, Integer.toString(trackID));
        e.setAttribute(ATTR_IS_ENCRYPTED, (isEncrypted ? "1" : "0"));
        if (isEncrypted) {
            e.setAttribute(ATTR_IV_SIZE, Integer.toString(ivSize));
            e.setAttribute(ATTR_FIRST_IV, "0x" + Hex.encodeHexString(iv));
            e.setAttribute(ATTR_SAI_BOX, "senc");
            if (keyRoll != -1)
                e.setAttribute(ATTR_KEY_ROLL, Integer.toString(keyRoll));
        }
        
        for (CryptKey key : keys) {
            e.appendChild(key.generateXML(d));
        }
        
        return e;
    }
}
