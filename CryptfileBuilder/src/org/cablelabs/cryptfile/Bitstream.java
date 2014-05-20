
package org.cablelabs.cryptfile;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Bitstream XML element used in definition of PSSH boxes in MP4Box cryptfiles
 */
public class Bitstream implements MP4BoxXML {
    
    private BSType type;
    private int bits;
    
    // Integers
    private int value;
    
    // Files
    private int offset;
    private int length;
    
    // All String values
    private String string;
    
    // Data
    private byte[] data;
    
    private enum BSType {
        VALUE,
        FILE,
        STRING,
        FOURCC,
        ID128,
        DATA64,
        DATA
    }
    
    private static final String ELEMENT = "BS";
    private static final String ATTR_BITS = "bits";
    private static final String ATTR_VALUE = "value";
    private static final String ATTR_STRING = "string";
    private static final String ATTR_FILE = "dataFile";
    private static final String ATTR_FILE_OFFSET = "dataOffset";
    private static final String ATTR_FILE_LENGTH = "dataLength";
    private static final String ATTR_FOURCC = "fcc";
    private static final String ATTR_ID128 = "ID128";
    private static final String ATTR_DATA64 = "data64";
    private static final String ATTR_DATA = "data";
    
    /**
     * An integer value stored in the given number of bits
     * 
     * @param value the integer value
     * @param bits the width of the field in bits (0-padded)
     */
    public Bitstream(int value, int bits) {
        type = BSType.VALUE;
        this.value = value;
        this.bits = bits;
    }
    
    /**
     * Contents of a file stored in the bitstream
     * 
     * @param file the file name
     * @param offset the offset into the file (0 for start of file)
     * @param length the length of data to write starting at the given offset (-1
     * to include all data to the end of the file)
     */
    public Bitstream(String file, int offset, int length) {
        type = BSType.FILE;
        string = file;
        this.offset = offset;
        this.length = length;
    }
    
    /**
     * UTF-8 encoded string value stored in the bitstream
     * 
     * @param string
     */
    public Bitstream(String string) {
        this(string, -1);
    }
    
    /**
     * ASCII string values stored in the bitstream preceded by an integer
     * value indicating the length of the string
     * 
     * @param string the string
     * @param bits the width of the length field in bits
     */
    public Bitstream(String string, int bits) {
        if (string.length() > (Math.pow(2, bits) - 1))
            throw new IllegalArgumentException("String length is too long for given bit width");
        type = BSType.STRING;
        this.string = string;
        this.bits = bits;
    }
    
    /**
     * Four Character Code (ASCII encoded)
     * 
     * @param fourcc the fourcc 
     */
    public Bitstream(char[] fourcc) {
        if (fourcc.length != 4)
            throw new IllegalArgumentException("FOURCC data is not 4 characters long!");
        type = BSType.FOURCC;
        this.string = new String(fourcc);
    }
    
    /**
     * 128-bit data value 
     * 
     * @param id128 the 128-bit value 
     */
    public Bitstream(byte[] id128) {
        if (id128.length != 16)
            throw new IllegalArgumentException("ID128 data is not 16 bytes in length!");
        type = BSType.ID128;
        this.data = id128;
    }
    
    /**
     * Arbitrary data to be encoded in either hexadecimal or base64
     * 
     * @param data the data
     * @param base64 true if data should be encoded in base64, false for hexadecimal
     */
    public Bitstream(byte[] data, boolean base64) {
        this(data, base64, -1);
    }
    
    /**
     * Arbitrary data to be encoded in either hexadecimal or base64 preceded by an integer
     * value indicating the length of the data
     * 
     * @param data the data
     * @param base64 true if data should be encoded in base64, false for hexadecimal
     * @param the width of the length field in bits
     */
    public Bitstream(byte[] data, boolean base64, int bits) {
        if (data.length > (Math.pow(2, bits) - 1))
            throw new IllegalArgumentException("Data length is too long for given bit width");
        type = base64 ? BSType.DATA64 : BSType.DATA;
        this.data = data;
        this.bits = bits;
    }
    
    /*
     * (non-Javadoc)
     * @see org.cablelabs.cryptfile.MP4BoxXML#generateXML(org.w3c.dom.Document)
     */
    @Override
    public Node generateXML(Document d) {
        
        Element e = d.createElement(ELEMENT);
        if (bits != -1)
            e.setAttribute(ATTR_BITS, Integer.toString(bits));
        
        switch (type) {
        case VALUE:
            e.setAttribute(ATTR_VALUE, Integer.toString(value));
            break;
        case FILE:
            e.setAttribute(ATTR_FILE, string);
            e.setAttribute(ATTR_FILE_OFFSET, Integer.toString(offset));
            e.setAttribute(ATTR_FILE_LENGTH, Integer.toString(length));
            break;
        case STRING:
            e.setAttribute(ATTR_STRING, string);
            break;
        case FOURCC:
            e.setAttribute(ATTR_FOURCC, string);
            break;
        case ID128:
            e.setAttribute(ATTR_ID128, Hex.encodeHexString(data));
            break;
        case DATA64:
            e.setAttribute(ATTR_DATA64, Base64.encodeBase64String(data));
            break;
        case DATA:
            e.setAttribute(ATTR_DATA, Hex.encodeHexString(data));
            break;
        default:
            break;
        }
        
        return e;
    }
}
