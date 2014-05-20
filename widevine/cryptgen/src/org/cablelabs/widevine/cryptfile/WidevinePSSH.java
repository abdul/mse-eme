
package org.cablelabs.widevine.cryptfile;

import org.cablelabs.cryptfile.DRMInfoPSSH;
import org.cablelabs.widevine.proto.WidevinePSSHProtoBuf;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Generates Widevine-specific PSSH for MP4Box cryptfiles
 */
public class WidevinePSSH extends DRMInfoPSSH {
    
    private static final byte[] WIDEVINE_SYSTEM_ID = {
        (byte)0xed, (byte)0xef, (byte)0x8b, (byte)0xa9,
        (byte)0x79, (byte)0xd6, (byte)0x4a, (byte)0xce,
        (byte)0xa3, (byte)0xc8, (byte)0x27, (byte)0xdc,
        (byte)0xd5, (byte)0x1d, (byte)0x21, (byte)0xed
    };
    
    private WidevinePSSHProtoBuf.WidevineCencHeader psshProto;
    
    /**
     * Create a new Widevine PSSH
     * 
     * @param psshProto
     */
    public WidevinePSSH(WidevinePSSHProtoBuf.WidevineCencHeader psshProto) {
        super(WIDEVINE_SYSTEM_ID);
        this.psshProto = psshProto;
    }

    /*
     * (non-Javadoc)
     * @see org.cablelabs.cryptfile.MP4BoxXML#generateXML(org.w3c.dom.Document)
     */
    @Override
    public Node generateXML(Document d) {
        Element e = generateDRMInfo(d);
    }

}
