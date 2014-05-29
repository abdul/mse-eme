
package org.cablelabs.clearkey.cryptfile;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.cablelabs.cryptfile.Bitstream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Generates CableLabs ClearKey "remote" PSSH for MP4Box cryptfiles.
 * This variant of the PSSH instructs the player to retrieve keys from
 * a remote server using a specific URL
 */
public class ClearKeyRemotePSSH extends ClearKeyPSSH {
    
    private URL url;
    private List<String> keyIDs;

    /**
     * Create a "remote" PSSH.  This PSSH instructs the player to retrieve
     * the keys from a remote server using the given URL
     * 
     * @param url the clearkey server url 
     * @param keyIDs a list of key IDs for keys that will be retrieved from
     * the server
     * @throws MalformedURLException if the URL could not be parsed
     */
    public ClearKeyRemotePSSH(String url, List<String> keyIDs) throws MalformedURLException {
        super();
        this.url = new URL(url);
        this.keyIDs = keyIDs;
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
        
        String urlString = url.toString();
        if (url.getPath() == null) {
            urlString += "/";
        }
        
        // Append query string
        urlString += "?";
        for (String keyID : keyIDs) {
            urlString += "keyid=" + keyID.replaceAll("/-/", "") + "&";
        }
        urlString = urlString.substring(0, urlString.length()-1);
        
        b.setupString(urlString, 16);
        e.appendChild(b.generateXML(d));
        
        return e;
    }
}
