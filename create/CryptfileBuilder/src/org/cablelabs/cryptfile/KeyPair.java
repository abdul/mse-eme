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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

/**
 * This class represents a 16-byte encryption key ID and a 16-byte AES-128 encryption
 * key.  The key ID is sometimes seen in GUID form (xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx),
 * and sometimes as a plain 16-byte array
 */
public class KeyPair {
    
    protected byte[] keyID;
    protected byte[] key;
    
    protected static final int AES_128_KEYSIZE = 16;
    protected static final int GUID_SIZE       = 16;
    
    // Converts a string GUID into a byte array
    public static byte[] parseGUID(String guid) {
        String[] parts = guid.split("-");
        if (parts.length != 5 || parts[0].length() != 8 || parts[1].length() != 4 ||
                parts[2].length() != 4 || parts[3].length() != 4 || parts[4].length() != 12)
            throw new IllegalArgumentException("Invalid GUID: " + guid);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream(GUID_SIZE);
        byte[] val;
        
        try {
            for (int i = 0; i < 5; i++) {
                val = Hex.decodeHex(parts[i].toCharArray());
                baos.write(val);
            }
        }
        catch (DecoderException e) {
            throw new IllegalArgumentException("Invalid GUID: " + guid);
        }
        catch (IOException e) {
            throw new IllegalArgumentException("Problem writing to ByteArrayOutputStream");
        }
        
        return baos.toByteArray();
    }
    
    private static byte[] parseHexKey(String hexKey) {
        try {
            return Hex.decodeHex(hexKey.toCharArray());
        }
        catch (DecoderException e) {
            throw new IllegalArgumentException("Invalid hex key value: " + e.getMessage());
        }
    }
    
    /**
     * Protected default constructor
     */
    protected KeyPair() {}
    
    /**
     * Protected constructor for validating the key
     * 
     * @param key the AES-128 key
     */
    protected KeyPair(byte[] key) {
        if (key.length != AES_128_KEYSIZE)
            throw new IllegalArgumentException("Invalid AES-128 key size: " + key.length);
        this.key = key;
    }
    
    /**
     * Creates a key pair from the given GUID of the form
     * (xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx) and the given key value,
     * 
     * @param guid the guid in standard form
     * @param key the 16-byte key value
     */
    public KeyPair(String guid, byte[] key) {
        this(key);
        keyID = parseGUID(guid);
    }
    
    /**
     * Creates a key pair from the given GUID of the form
     * (xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx) and the given key value
     * in hexadecimal
     * 
     * @param guid the guid in standard form
     * @param key the 16-byte key value in hex
     */
    public KeyPair(String guid, String key) {
        this(parseHexKey(key));
        keyID = parseGUID(guid);
    }
    
    /**
     * Creates a key pair from the given key id and key values
     * 
     * @param keyID the key ID in binary form
     * @param key the 16-byte key value
     */
    public KeyPair(byte[] keyID, byte[] key) {
        this(key);
        this.keyID = keyID;
    }
    
    /**
     * Returns the 16-byte key ID
     * 
     * @return the key ID
     */
    public byte[] getID() {
        return keyID;
    }
    
    /**
     * Returns the 16-byte key
     * 
     * @return the key
     */
    public byte[] getKey() {
        return key;
    }
}
