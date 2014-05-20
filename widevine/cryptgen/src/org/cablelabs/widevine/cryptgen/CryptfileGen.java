
package org.cablelabs.widevine.cryptgen;

import java.util.Vector;

import org.cablelabs.widevine.Track;
import org.cablelabs.widevine.TrackType;
import org.cablelabs.widevine.keyreq.KeyRequest;
import org.cablelabs.widevine.keyreq.ResponseMessage;

public class CryptfileGen {

    private static void usage() {
        System.out.println("usage:  WidevineKeyRequest [-s] <content_id> <track_id>:<track_type> [<track_id>:<track_type>...]");
        System.out.println("\t OPTIONS:");
        System.out.println("\t\t -s If present, the request will signed with the CableLabs' key");
        System.out.println("");
        System.out.println("\t <content_id> is a unique string representing the content to be encrypted");
        System.out.println("\t <track_id> is the track ID from the MP4 file to be encrypted");
        System.out.println("\t <track_type> is one of HD, SD, or AUDIO describing the type of the associated track");
    }
    
    public static void main(String[] args) {

        // Track list
        Vector<Track> tracks = new Vector<Track>();
        
        // Should the request be signed
        boolean sign_request = false;
        
        // Parse arguments
        String content_id_str = null;
        for (int i = 0; i < args.length; i++) {
            
            // Parse options
            if (args[i].startsWith("-")) {
                if (args[i].equals("-s")) {
                    sign_request = true;
                    content_id_str = args[++i];
                }
                else {
                    usage();
                    System.err.println("Illegal argument: " + args[i]);
                    System.exit(1);;
                }
                
                continue;
            }
            
            // Get content ID
            if (content_id_str == null) {
                content_id_str = args[i];
                continue;
            }
            
            // Parse tracks
            String track_desc[] = args[i].split(":");
            if (track_desc.length != 2) {
                usage();
                System.exit(1);;
            }
            try {
                Track t = new Track();
                t.type = TrackType.valueOf(track_desc[1]);
                t.id = Integer.parseInt(track_desc[0]);
                tracks.add(t);
            }
            catch (IllegalArgumentException e) {
                usage();
                System.err.println("Illegal track_type -- " + track_desc[1]);
                System.exit(1);;
            }
        }
        
        KeyRequest request = new KeyRequest(content_id_str, tracks, sign_request);
        ResponseMessage m = request.requestKeys();
    
    }
}
