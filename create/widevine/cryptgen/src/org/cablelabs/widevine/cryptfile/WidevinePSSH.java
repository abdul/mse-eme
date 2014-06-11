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

package org.cablelabs.widevine.cryptfile;

import org.apache.commons.codec.binary.Base64;
import org.cablelabs.cryptfile.Bitstream;
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
        Bitstream b = new Bitstream();
        b.setupDataB64(Base64.encodeBase64String(psshProto.toByteArray()));
        e.appendChild(b.generateXML(d));
        return e;
    }
}
