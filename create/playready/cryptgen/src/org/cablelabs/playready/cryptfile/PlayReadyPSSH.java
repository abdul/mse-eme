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

package org.cablelabs.playready.cryptfile;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.cablelabs.cryptfile.Bitstream;
import org.cablelabs.cryptfile.DRMInfoPSSH;
import org.cablelabs.playready.WRMHeader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Generates PlayReady-specific PSSH for MP4Box cryptfiles
 */
public class PlayReadyPSSH extends DRMInfoPSSH {
    
    private static final byte[] PLAYREADY_SYSTEM_ID = {
        (byte)0x9a, (byte)0x04, (byte)0xf0, (byte)0x79,
        (byte)0x98, (byte)0x40, (byte)0x42, (byte)0x86,
        (byte)0xab, (byte)0x92, (byte)0xe6, (byte)0x5b,
        (byte)0xe0, (byte)0x88, (byte)0x5f, (byte)0x95
    };
    
    private List<WRMHeader> wrmHeaders;

    public PlayReadyPSSH(List<WRMHeader> wrmHeaders) {
        super(PLAYREADY_SYSTEM_ID);
        this.wrmHeaders = wrmHeaders;
    }

    /*
     * (non-Javadoc)
     * @see org.cablelabs.cryptfile.MP4BoxXML#generateXML(org.w3c.dom.Document)
     */
    @Override
    public Node generateXML(Document d) {
        List<byte[]> wrmheader_data = new ArrayList<byte[]>();
        
        // Collect all of our WRMHeader data arrays so that we can calculate the
        // total size
        int wrmSize = 0;
        for (WRMHeader header : wrmHeaders) {
            byte[] data = header.getWRMHeaderData();
            wrmSize += data.length;
            wrmheader_data.add(data);
        }
        
        // Total size of PlayReady Header Object is:
        // 
        //    4          PlayReady Header Object Size field
        //    2          Number of Records field
        //    4*NumRec   Record Type and Record Length field for each record
        //    RecSize    Size of all headers
        int proSize = 4 + 2 + (4*wrmHeaders.size()) + wrmSize;
        
        Element e = generateDRMInfo(d);
        Bitstream b = new Bitstream();
        
        // PlayReady Header Object Size field
        b.setupIntegerLE(proSize, 32);
        e.appendChild(b.generateXML(d));
        
        // Number of Records field
        b.setupIntegerLE(wrmHeaders.size(), 16);
        e.appendChild(b.generateXML(d));
        
        for (byte[] wrmData : wrmheader_data) {
            
            // Record Type (always 1 for WRM Headers)
            b.setupIntegerLE(1, 16);
            e.appendChild(b.generateXML(d));
            
            // Record Length
            b.setupIntegerLE(wrmData.length, 16);
            e.appendChild(b.generateXML(d));
            
            // Data
            b.setupDataB64(Base64.encodeBase64String(wrmData));
            e.appendChild(b.generateXML(d));
        }
        
        return e;
    }
}
