// COPYRIGHT_BEGIN
// COPYRIGHT_END

package org.cablelabs.widevine;

/**
 * Widevine JSON request object
 */
public class RequestMessage {
    
    static class Track {
        TrackType type;
    }

    String content_id;
    String policy;
    String client_id;
    String drm_types[];
    Track tracks[];
    String token;
    String rsa_public_key;
}
