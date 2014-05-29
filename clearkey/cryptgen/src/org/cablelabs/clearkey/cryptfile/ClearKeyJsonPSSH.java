
package org.cablelabs.clearkey.cryptfile;

import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.cablelabs.clearkey.ClearKeyJWK;
import org.cablelabs.cryptfile.Bitstream;
import org.cablelabs.cryptfile.KeyPair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Generates CableLabs ClearKey "JSON" PSSH for MP4Box cryptfiles.
 * This variant of the PSSH informs the player that the decryption
 * keys are embedded directly in the PSSH in JSON format expected
 * by W3C Encrypted Media Extensions for ClearKey.
 */
public class ClearKeyJsonPSSH extends ClearKeyPSSH {
    
    private List<KeyPair> keypairs;

    /**
     * Create a "JSON" PSSH.  This PSSH contains the keyIDs and
     * decryption keys in JSON Web Key format
     * 
     * @param keypairs a list of keypairs
     */
    public ClearKeyJsonPSSH(List<KeyPair> keypairs) {
        super();
        this.keypairs = keypairs;
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
        
        // Create the JWK Object and populate it with our keys
        ClearKeyJWK jwk = new ClearKeyJWK();
        jwk.keys = new ClearKeyJWK.Key[keypairs.size()];
        int i = 0;
        for (KeyPair keypair : keypairs) {
            ClearKeyJWK.Key key = new ClearKeyJWK.Key();
            
            // Remove the padding characters from the Base64 encode
            key.kid = Base64.encodeBase64String(keypair.getID()).replace("/=+$/", "");
            key.k = Base64.encodeBase64String(keypair.getKey()).replace("/=+$/", "");
            
            jwk.keys[i++] = key;
        }

        // Generate the element from the JSON string
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        b.setupString(gson.toJson(jwk), 16);
        e.appendChild(b.generateXML(d));
        
        return e;
    }
}
