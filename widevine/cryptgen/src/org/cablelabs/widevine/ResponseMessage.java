// Confidential material under the terms of the Limited Distribution Non-disclosure
// Agreement between CableLabs and Comcast

package org.cablelabs.widevine;

public class ResponseMessage {
    
    public enum StatusCode {
        OK,
        SIGNATURE_FAILED,
        CONTENT_ID_MISSING,
        POLICY_UNKNOWN,
        TRACK_TYPE_MISSING,
        TRACK_TYPE_UNKNOWN,
        MALFORMED_REQUEST,
    }
    
    static class DRM {
        String type;
        String system_id;
    }
    
    static class Track {
        static class PSSH {
            String drm_type;
            String pssh_data;
        }
        
        PSSH pssh[];
        String key_id;
        TrackType type;
        String key;
    }
    
    StatusCode status;
    String content_id;
    String session_key;
    DRM drm[];
    Track tracks[];
}
