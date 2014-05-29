
package org.cablelabs.clearkey.cryptfile;

import org.cablelabs.cryptfile.DRMInfoPSSH;

/**
 * Abstract base class for CableLabs ClearKey PSSH variants
 */
public abstract class ClearKeyPSSH extends DRMInfoPSSH {
    
    private static final byte[] CLEARKEY_SYSTEM_ID = {
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
    };
    
    protected ClearKeyPSSH() {
        super(CLEARKEY_SYSTEM_ID);
    }
    
}
