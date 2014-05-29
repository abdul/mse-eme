
package org.cablelabs.cryptfile;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Bitstream XML element used in definition of PSSH boxes in MP4Box cryptfiles.
 * 
 * To use, construct a default Bitstream, then use one of the setup* methods to set
 * data of a particular type.  You can re-use a single instance of this object
 * multiple times to generate multiple XML elements.
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
    byte[] data;
    
    private enum BSType {
        VALUE,
        VALUE_LE,
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
    private static final String ATTR_ENDIAN = "endian";
    private static final String ATTR_STRING = "string";
    private static final String ATTR_FILE = "dataFile";
    private static final String ATTR_FILE_OFFSET = "dataOffset";
    private static final String ATTR_FILE_LENGTH = "dataLength";
    private static final String ATTR_FOURCC = "fcc";
    private static final String ATTR_ID128 = "ID128";
    private static final String ATTR_DATA64 = "data64";
    private static final String ATTR_DATA = "data";
    
    private void setupIntegerInternal(BSType type, int value, int bits) {
        this.type = type;
        this.value = value;
        this.bits = bits;
    }
    
    /**
     * An integer value stored in the given number of bits
     * 
     * @param value the integer value
     * @param bits the width of the field in bits (0-padded)
     */
    public void setupInteger(int value, int bits) {
        setupIntegerInternal(BSType.VALUE, value, bits);
    }
    
    /**
     * An integer value stored in the given number of bits in
     * little-endian format
     * 
     * @param value the integer value
     * @param bits the width of the field in bits (0-padded)
     */
    public void setupIntegerLE(int value, int bits) {
        setupIntegerInternal(BSType.VALUE_LE, value, bits);
    }
    
    /**
     * Contents of a file stored in the bitstream
     * 
     * @param file the file name
     * @param offset the offset into the file (0 for start of file)
     * @param length the length of data to write starting at the given offset (-1
     * to include all data to the end of the file)
     */
    public void setupFile(String file, int offset, int length) {
        type = BSType.FILE;
        string = file;
        this.offset = offset;
        this.length = length;
        bits = 0;
    }
    
    /**
     * UTF-8 encoded string value stored in the bitstream
     * 
     * @param string
     */
    public void setupString(String string) {
        type = BSType.STRING;
        this.string = string;
        bits = 0;
    }
    
    /**
     * ASCII string values stored in the bitstream preceded by an integer
     * value indicating the length of the string
     * 
     * @param string the string
     * @param bits the width of the length field in bits
     */
    public void setupString(String string, int bits) {
        if (string.length() > (Math.pow(2, bits) - 1))
            throw new IllegalArgumentException("String length is too long for given bit width");
        setupString(string);
        this.bits = bits;
    }
    
    /**
     * Four Character Code (ASCII encoded)
     * 
     * @param fourcc the fourcc 
     */
    public void setupFourCC(char[] fourcc) {
        if (fourcc.length != 4)
            throw new IllegalArgumentException("FOURCC data is not 4 characters long!");
        type = BSType.FOURCC;
        string = new String(fourcc);
        bits = 0;
    }
    
    /**
     * 128-bit data value 
     * 
     * @param id128 the 128-bit value 
     */
    public void setupID128(byte[] id128) {
        if (id128.length != 16)
            throw new IllegalArgumentException("ID128 data is not 16 bytes in length!");
        type = BSType.ID128;
        data = id128;
        bits = 0;
    }
    
    /**
     * Arbitrary data.  Will be encoded to hexadecimal string for insertion in the cryptfile
     * 
     * @param data the data
     */
    public void setupData(byte[] data) {
        type = BSType.DATA;
        this.data = data;
        bits = 0;
    }
    
    /**
     * Arbitrary data preceded by an integer value indicating the length of the data.  Data
     * will be encoded to hexadecimal string for insertion in the cryptfile
     * 
     * @param data the data
     * @param the width of the length field in bits
     */
    public void setupData(byte[] data, int bits) {
        if (data.length > (Math.pow(2, bits) - 1))
            throw new IllegalArgumentException("Data length is too long for given bit width");
        setupData(data);
        this.bits = bits;
    }
    
    /**
     * Arbitrary data.  Input string hexadecimal will be used "as is" in the cryptfile.
     * 
     * @param hexData the data in hexadecimal string format
     * @throws DecoderException 
     */
    public void setupDataHex(String hexData) throws DecoderException {
        type = BSType.DATA;
        data = Hex.decodeHex(hexData.toCharArray());
        bits = 0;
    }
    
    /**
     * Arbitrary data preceded by an integer value indicating the length of the data.  Input
     * string hexadecimal will be used "as is" in the cryptfile
     * 
     * @param hexData the data in hexadecimal string format
     * @param the width of the length field in bits
     * @throws DecoderException 
     */
    public void setupDataHex(String hexData, int bits) throws DecoderException {
        if ((hexData.length() / 2) > (Math.pow(2, bits) - 1))
            throw new IllegalArgumentException("Data length is too long for given bit width");
        setupDataHex(hexData);
        this.bits = bits;
    }
    
    /**
     * Arbitrary data.  Input string in base64 notation will be used "as is" in the cryptfile
     * 
     * @param data the data
     */
    public void setupDataB64(String b64Data) {
        type = BSType.DATA64;
        data = Base64.decodeBase64(b64Data);
        bits = 0;
    }
    
    /**
     * Arbitrary data preceded by an integer value indicating the length of the data
     * 
     * @param data the data in hexadecimal string format
     * @param the width of the length field in bits
     */
    public void setupDataB64(String b64Data, int bits) {
        if (Base64.decodeBase64(b64Data).length > (Math.pow(2, bits) - 1))
            throw new IllegalArgumentException("Data length is too long for given bit width");
        setupDataB64(b64Data);
        this.bits = bits;
    }
    
    /*
     * (non-Javadoc)
     * @see org.cablelabs.cryptfile.MP4BoxXML#generateXML(org.w3c.dom.Document)
     */
    @Override
    public Node generateXML(Document d) {
        
        Element e = d.createElement(ELEMENT);
        if (bits != 0)
            e.setAttribute(ATTR_BITS, Integer.toString(bits));
        
        switch (type) {
        case VALUE_LE:
            e.setAttribute(ATTR_ENDIAN, "little");
            // fall through
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
