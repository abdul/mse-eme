
package org.cablelabs.cryptfile;

import java.util.List;

import org.w3c.dom.Node;

/**
 * This class is used to generate "cryptfiles" for MP4Box media encryption
 */
public class CryptfileBuilder {
    
    private ProtectionScheme scheme;

    /**
     * Possible encryption schemes under Common Encryption
     */
    public enum ProtectionScheme {
        AES_CTR,
        AES_CBC
    }
    
    public CryptfileBuilder(ProtectionScheme scheme, List<CryptTrack> tracks) {
        this.scheme = scheme;
    }
    
    /**
     * Specifies a new PSSH that will be included in the cryptfile.
     * 
     * @param systemID The DRM-specific system ID that will be included in the PSSH
     * @param bsElements A list of &lt;BS&gt; elements that describe the PSSH bitstream
     * contents <a href="http://gpac.wp.mines-telecom.fr/mp4box/media-import/nhml-format/">
     * as documented on the MP4Box website</a>.
     */
    public void addPSSH(byte systemID, List<Node> bsElements) {
        
    }
}
