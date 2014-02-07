// COPYRIGHT_BEGIN
// COPYRIGHT_END

package org.cablelabs.widevine;

public class Response {
    
    public enum StatusCode {
        
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
