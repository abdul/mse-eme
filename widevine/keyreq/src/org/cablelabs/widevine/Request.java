// COPYRIGHT_BEGIN
// COPYRIGHT_END

package org.cablelabs.widevine;

/**
 * Widevine JSON request object
 */
public class Request {
    
    public static class Track {
        public enum Type {
            HD,
            SD,
            AUDIO
        };
        
    }

    String content_id;
    String policy;
    String client_id;
    Track tracks[];
    
}
