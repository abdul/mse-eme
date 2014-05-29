
package org.cablelabs.playready;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.cablelabs.cryptfile.KeyPair;

/**
 * PlayReady Common Encryption Key Object.
 * 
 * Consists of a 16-byte keyID, a 16-byte key value, and an 8-byte checksum
 * corresponding to the AESCTR algorithm ID
 */
public class PlayReadyKeyPair extends KeyPair {
    
    private byte[] mskeyID;
    private byte[] checksum;
    
    // Default key seed is the one used by the Microsoft test server
    private byte[] keySeed = {
            (byte)0x5D, (byte)0x50, (byte)0x68, (byte)0xBE,
            (byte)0xC9, (byte)0xB3, (byte)0x84, (byte)0xFF,
            (byte)0x60, (byte)0x44, (byte)0x86, (byte)0x71,
            (byte)0x59, (byte)0xF1, (byte)0x6D, (byte)0x6B,
            (byte)0x75, (byte)0x55, (byte)0x44, (byte)0xFC,
            (byte)0xD5, (byte)0x11, (byte)0x69, (byte)0x89,
            (byte)0xB1, (byte)0xAC, (byte)0xC4, (byte)0x27,
            (byte)0x8E, (byte)0x88 
    };
    
    /**
     * Converts a GUID into the Microsoft-specific binary encoded form as described
     * <a href="http://en.wikipedia.org/wiki/Globally_unique_identifier#Binary_encoding">here</a>
     * for little-endian platforms
     * 
     * @param guid the 16-byte GUID
     * @return the binary encoded GUID for little-endian platforms
     */
    private byte[] binaryEncodeMSGUID(byte[] guid) {
        if (guid.length != 16)
            throw new IllegalArgumentException("Illegal GUID length: " + guid.length);
        
        byte[] retVal = new byte[GUID_SIZE];
        
        int pos = 0;
        for (int j = 4; j > 0; j--)
            retVal[pos++] = guid[j-1];
        for (int j = 6; j > 4; j--)
            retVal[pos++] = guid[j-1];
        for (int j = 8; j > 6; j--)
            retVal[pos++] = guid[j-1];
        for (int j = 8; j < 16; j++)
            retVal[pos++] = guid[j];
        
        return retVal;
    }
    
    // Generate the key from the key ID according to the Microsoft-documented algorithm
    private static byte[] generateKey(byte[] keyID, byte[] keySeed) {
        byte[] key = new byte[AES_128_KEYSIZE];
        
        try {
            
            // First hash is
            //     - Key Seed
            //     - Key ID
            MessageDigest sha256_a = MessageDigest.getInstance("SHA-256");
            sha256_a.update(keySeed);
            sha256_a.update(keyID);
            byte[] sha_a = sha256_a.digest();
            
            // Second hash is
            //     - Key Seed
            //     - Key ID
            //     - Key Seed
            MessageDigest sha256_b = MessageDigest.getInstance("SHA-256");
            sha256_b.update(keySeed);
            sha256_b.update(keyID);
            sha256_b.update(keySeed);
            byte[] sha_b = sha256_b.digest();
            
            // Third hash is
            //     - Key Seed
            //     - Key ID
            //     - Key Seed
            //     - Key ID
            MessageDigest sha256_c = MessageDigest.getInstance("SHA-256");
            sha256_c.update(keySeed);
            sha256_c.update(keyID);
            sha256_c.update(keySeed);
            sha256_c.update(keyID);
            byte[] sha_c = sha256_c.digest();
            
            for (int i = 0; i < AES_128_KEYSIZE; i++) {
                key[i] = (byte)
                        (sha_a[i] ^ sha_a[i + AES_128_KEYSIZE] ^
                        sha_b[i] ^ sha_b[i + AES_128_KEYSIZE] ^
                        sha_c[i] ^ sha_c[i + AES_128_KEYSIZE]);
            }
        }
        catch (NoSuchAlgorithmException e) {
            System.out.println("Java Virtual Machine does not support SHA-256 algorithm!");
        }
        
        return key;
    }
    
    // Generate the key/keyID checksum according to the Microsoft documentation
    private static byte[] generateChecksum(byte[] mskeyID, byte[] key) {
        byte[] checksum = null;
        // Generate checksum
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            checksum = Arrays.copyOf(cipher.doFinal(mskeyID), 8);
        }
        catch (NoSuchAlgorithmException e) {
            System.out.println("Java Virtual Machine does not support AES/ECB cipher!");
        }
        catch (NoSuchPaddingException e) {
            System.out.println("Java Virtual Machine does not support NoPadding!");
        }
        catch (InvalidKeyException e) {
            System.out.println("Invalid key during checksum generation!");
        }
        catch (IllegalBlockSizeException e) {
            System.out.println("Illegal block size exception during checksum generation!");
        }
        catch (BadPaddingException e) {
            System.out.println("Bad padding exception during checksum generation!");
        }
        return checksum;
    }
    
    /**
     * Create a key from pre-existing key data
     * 
     * @param keyID the key ID GUID in xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx format
     * @param key the 16-byte key value
     * @param checksum the 8-byte checksum
     * @param keySeed the key seed used to generate the key (can be null)
     */
    public PlayReadyKeyPair(String keyID, byte[] key, byte[] checksum, byte[] keySeed) {
        super(keyID, key);
        this.mskeyID = binaryEncodeMSGUID(this.keyID);
        this.checksum = generateChecksum(mskeyID, key);
        this.keySeed = keySeed;
    }

    /**
     * Create a key in which the encryption key will be generated from the
     * given key seed and given key ID using the algorithm
     * <a href="http://download.microsoft.com/download/2/0/2/202E5BD8-36C6-4DB8-9178-12472F8B119E/PlayReady%20Header%20Object%204-15-2013.docx>documented by Microsoft</a>
     * 
     * @param keyID the key ID GUID in xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx format
     * @param keySeed
     */
    public PlayReadyKeyPair(String keyID, byte[] keySeed) {
        super();
        this.keyID = parseGUID(keyID);
        this.mskeyID = binaryEncodeMSGUID(this.keyID);
        this.key = generateKey(this.mskeyID, keySeed);
        this.checksum = generateChecksum(mskeyID, key);
        this.keySeed = keySeed;
    }
    
    /**
     * Create a key in which the encryption key will be generated from the
     * key seed specified on the
     * <a href="http://playready.directtaps.net/pr/doc/customrights/>PlayReady Test Server</a>
     * and given key ID using the algorithm
     * <a href="http://download.microsoft.com/download/2/0/2/202E5BD8-36C6-4DB8-9178-12472F8B119E/PlayReady%20Header%20Object%204-15-2013.docx>documented by Microsoft</a>
     * 
     * @param keyID the key ID GUID in xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx format
     */
    public PlayReadyKeyPair(String keyID) {
        super();
        this.keyID = parseGUID(keyID);
        this.mskeyID = binaryEncodeMSGUID(this.keyID);
        this.key = generateKey(this.mskeyID, keySeed);
        this.checksum = generateChecksum(mskeyID, key);
    }

    /**
     * Returns the 16-byte, Microsoft binary encoded, Key ID
     * 
     * @return the MS-encoded key ID
     */
    public byte[] getMSKeyID() {
        return mskeyID;
    }

    /**
     * Returns the 8-byte checksum for AESCTR algorithm ID
     * 
     * @return the checksum
     */
    public byte[] getChecksum() {
        return checksum;
    }

    /**
     * Returns the key seed used to generate the key and checksum or null if no
     * key seed was used
     * 
     * @return the key seed
     */
    public byte[] getKeySeed() {
        return keySeed;
    }
    
}
