// Confidential material under the terms of the Limited Distribution Non-disclosure
// Agreement between CableLabs and Comcast

package org.cablelabs.widevine.keyreq;

import org.cablelabs.widevine.TrackType;

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
    
    static public class DRM {
        public String type;
        public String system_id;
    }
    
    static public class Track {
        static public class PSSH {
            public String drm_type;
            public String data;
        }
        
        public PSSH pssh[];
        public String key_id;
        public TrackType type;
        public String key;
    }
    
    public StatusCode status;
    public String content_id;
    public String session_key;
    public DRM drm[];
    public Track tracks[];
    public boolean already_used;
}
