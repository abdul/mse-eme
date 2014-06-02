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

package org.cablelabs.playready.keygen;

import java.util.Arrays;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.codec.binary.Base64;

public class PlayReadyKeygen {
    
    private static final int DRM_AES_KEYSIZE_128 = 16;
    private static final String DEFAULT_KEY_SEED = "XVBovsmzhP9gRIZxWfFta3VVRPzVEWmJsazEJ46I";

    private static void usage() {
        System.out.println("usage:  PlayReadyKeygen <key_id> [<key_seed>]");
        System.out.println("\t <key_id> is a UUID representing the Key ID in the form xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx");
        System.out.println("\t\t where 'x' is any hexadecimal digit in the range [0-9a-f] (case insensitive)");
        System.out.println("\t <key_seed> is a 30-byte key seed value in Base64 notation.  If not provided, the default");
        System.out.println("\t\t value of the Microsoft test server is used (" + DEFAULT_KEY_SEED + ")");
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        
        // Make sure we have at least 1 arg
        if (args.length < 1) {
            usage();
            System.exit(1);
        }
        
        String key_id_str = args[0];
        String key_seed_str = DEFAULT_KEY_SEED;
        if (args.length > 1) {
            key_seed_str = args[1];
        }

        // Check for valid UUID format of key ID
        if (key_id_str.length() != 36 ||
                key_id_str.charAt(8) != '-' || key_id_str.charAt(13) != '-' ||
                key_id_str.charAt(18) != '-' || key_id_str.charAt(23) != '-') {
            usage();
            System.out.println("****** Invalid key ID (UUID format)!");
            System.exit(1);;
        }
        
        // Parse key ID 
        key_id_str = key_id_str.replaceAll("-", "");
        byte[] key_id = null;
        try {
            key_id = DatatypeConverter.parseHexBinary(key_id_str);
        }
        catch (IllegalArgumentException e) {
            System.out.println("******  Invalid key ID (not valid Base64 string)!");
            System.exit(1);
        }
        
        // Key ID in the PlayReady Header object must be little endian 
        byte[] ms_key_id = new byte[key_id.length];
        int le_pos = 0;
        for (int j = 4; j > 0; j--)
            ms_key_id[le_pos++] = key_id[j-1];
        for (int j = 6; j > 4; j--)
            ms_key_id[le_pos++] = key_id[j-1];
        for (int j = 8; j > 6; j--)
            ms_key_id[le_pos++] = key_id[j-1];
        for (int j = 8; j < 16; j++)
            ms_key_id[le_pos++] = key_id[j];
        
        // Ensure that key seed is at least 30 bytes in length
        byte[] key_seed_raw = Base64.decodeBase64(key_seed_str);
        if (key_seed_raw.length < 30) {
            usage();
            System.out.println("******  Invalid key seed ( < 30 bytes )!");
            System.exit(1);;
        }
        
        // Truncate to 30 bytes
        byte[] key_seed = Arrays.copyOf(key_seed_raw, 30);
        
        byte[] contentKey = new byte[DRM_AES_KEYSIZE_128];
        try {
            
            // First hash is
            //     - Key Seed
            //     - Key ID
            MessageDigest sha256_a = MessageDigest.getInstance("SHA-256");
            sha256_a.update(key_seed);
            sha256_a.update(ms_key_id);
            byte[] sha_a = sha256_a.digest();
            
            // Second hash is
            //     - Key Seed
            //     - Key ID
            //     - Key Seed
            MessageDigest sha256_b = MessageDigest.getInstance("SHA-256");
            sha256_b.update(key_seed);
            sha256_b.update(ms_key_id);
            sha256_b.update(key_seed);
            byte[] sha_b = sha256_b.digest();
            
            // Third hash is
            //     - Key Seed
            //     - Key ID
            //     - Key Seed
            //     - Key ID
            MessageDigest sha256_c = MessageDigest.getInstance("SHA-256");
            sha256_c.update(key_seed);
            sha256_c.update(ms_key_id);
            sha256_c.update(key_seed);
            sha256_c.update(ms_key_id);
            byte[] sha_c = sha256_c.digest();
            
            for (int i = 0; i < DRM_AES_KEYSIZE_128; i++) {
                contentKey[i] = (byte)
                        (sha_a[i] ^ sha_a[i + DRM_AES_KEYSIZE_128] ^
                        sha_b[i] ^ sha_b[i + DRM_AES_KEYSIZE_128] ^
                        sha_c[i] ^ sha_c[i + DRM_AES_KEYSIZE_128]);
            }
        }
        catch (NoSuchAlgorithmException e) {
            System.out.println("Java Virtual Machine does not support SHA-256 algorithm!");
            System.exit(1);
        }
        
        // Generate checksum
        byte checksum[] = null;
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            SecretKeySpec key = new SecretKeySpec(contentKey, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            checksum = Arrays.copyOf(cipher.doFinal(ms_key_id), 8);
        }
        catch (NoSuchAlgorithmException e) {
            System.out.println("Java Virtual Machine does not support AES/ECB cipher!");
            System.exit(1);
        }
        catch (NoSuchPaddingException e) {
            System.out.println("Java Virtual Machine does not support NoPadding!");
            System.exit(1);
        }
        catch (InvalidKeyException e) {
            System.out.println("Invalid key during checksum generation!");
            System.exit(1);
        }
        catch (IllegalBlockSizeException e) {
            System.out.println("Illegal block size exception during checksum generation!");
            e.printStackTrace();
        }
        catch (BadPaddingException e) {
            System.out.println("Bad padding exception during checksum generation!");
            e.printStackTrace();
        }
        
        // Now generate a random 8-byte IV
        byte[] iv = new byte[8];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(iv);
            
        System.out.println("===============================================");
        System.out.println("Content key ID = ");
        System.out.println("\t0x" + DatatypeConverter.printHexBinary(key_id));
        System.out.println("\t0x" + DatatypeConverter.printHexBinary(ms_key_id) + " (MS binary)");
        System.out.println("\t" + Base64.encodeBase64String(key_id) + " (Base64)");
        System.out.println("\t" + Base64.encodeBase64String(ms_key_id) + " (Base64, MS binary)");
        System.out.println("Content key = ");
        System.out.println("\t0x" + DatatypeConverter.printHexBinary(contentKey));
        System.out.println("\t" + Base64.encodeBase64String(contentKey) + " (Base64)");
        System.out.println("Checksum = ");
        System.out.println("\t0x" + DatatypeConverter.printHexBinary(checksum));
        System.out.println("\t" + Base64.encodeBase64String(checksum) + " (Base64)");
        System.out.println("IV = ");
        System.out.println("\t0x" + DatatypeConverter.printHexBinary(iv));
        System.out.println("\t" + Base64.encodeBase64String(iv) + " (Base64)");
        System.out.println("===============================================");
        
    }

}
