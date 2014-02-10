// COPYRIGHT_BEGIN
// COPYRIGHT_END

package org.cablelabs.widevine;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.codec.binary.Base64;

public class WidevineKeyRequest {

    private static void usage() {
        System.out.println("usage:  WidevineKeyRequest <content_id> <crypt_xml> <track_id>:<track_type> [<track_id>:<track_type>...]");
        System.out.println("\t <content_id> is a unique string representing the content to be encrypted");
        System.out.println("\t <crypt_xml> is a filename where the GPAC XML crypt file will be written");
        System.out.println("\t <track_id> is the track ID from the MP4 file to be encrypted");
        System.out.println("\t <track_type> is one of HD, SD, or AUDIO describing the type of the associated track");
    }
    
    private static final String POLICY = "";
    private static final String CLIENT_ID = null;
    private static final String[] DRM_TYPES = { "WIDEVINE" };
    
    private static final String SERVER_URL = "http://license.uat.widevine.com/cenc/getcontentkey/widevine_test";
    
    public static void main(String[] args) {
        
        int i;

        // Make sure we have 2 args
        if (args.length < 3) {
            usage();
            System.exit(1);
        }
        
        String content_id_str = args[0];
        String crypt_filename = args[1];
        
        // Map track type to track ID
        Map<TrackType, String> tracks = new HashMap<TrackType, String>();
        
        // Parse track arguments
        for (i = 2; i < args.length; i++) {
            String track_desc[] = args[i].split(":");
            if (track_desc.length != 2) {
                usage();
                System.exit(1);;
            }
            try {
                TrackType type = TrackType.valueOf(track_desc[1]);
                tracks.put(type, track_desc[0]);
            }
            catch (IllegalArgumentException e) {
                usage();
                System.err.println("Illegal track_type -- " + track_desc[1]);
                System.exit(1);;
            }
        }
        
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        Gson prettyGson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

        // Create request object
        RequestMessage requestMessage = new RequestMessage();
        requestMessage.content_id = Base64.encodeBase64String(content_id_str.getBytes());
        requestMessage.policy = POLICY;
        requestMessage.client_id = CLIENT_ID;
        requestMessage.drm_types = DRM_TYPES;
        
        // Add the track requests to the message
        Set<TrackType> types = tracks.keySet();
        requestMessage.tracks = new RequestMessage.Track[types.size()];
        i = 0;
        for (TrackType t : types) {
            RequestMessage.Track track = new RequestMessage.Track();
            track.type = t;
            requestMessage.tracks[i++] = track;
        }
        
        // Convert request message to JSON and base64 encode
        String jsonRequestMessage = gson.toJson(requestMessage);
        System.out.println("Request Message:");
        System.out.println(prettyGson.toJson(requestMessage));
        String jsonRequestMessageB64 = Base64.encodeBase64String(jsonRequestMessage.getBytes());
        
        // Create request JSON
        Request request = new Request();
        request.request = jsonRequestMessageB64;
        String jsonRequest = gson.toJson(request);
        System.out.println("Request:");
        System.out.println(prettyGson.toJson(request));
        
        String jsonResponseStr = null;
        try {
            
            // Create URL connection
            URL url = new URL(SERVER_URL);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            
            System.out.println("Sending HTTP POST to " + SERVER_URL);
            
            // Write POST data
            DataOutputStream out = new DataOutputStream(con.getOutputStream());
            out.writeBytes(jsonRequest);
            out.flush();
            out.close();
            
            // Wait for response
            int responseCode = con.getResponseCode();
            System.out.println("Received response code -- " + responseCode);
            
            // Read response data
            DataInputStream dis = new DataInputStream(con.getInputStream());
            int bytesRead;
            byte responseData[] = new byte[1024];
            StringBuffer sb = new StringBuffer();
            while ((bytesRead = dis.read(responseData)) != -1) {
                sb.append(new String(responseData,0,bytesRead));
            }
            jsonResponseStr = sb.toString();
        }
        catch (Exception e) {
            System.err.println("Error in HTTP communication! -- " + e.getMessage());
            System.exit(1);
        }
        
        Response response = gson.fromJson(jsonResponseStr, Response.class);
        System.out.println("Response:");
        System.out.println(prettyGson.toJson(response));
        String responseMessageStr = new String(Base64.decodeBase64(response.response));
        ResponseMessage responseMessage = gson.fromJson(responseMessageStr, ResponseMessage.class);
        System.out.println("ResponseMessage:");
        System.out.println(prettyGson.toJson(responseMessage));
        ResponseMessage newresp = responseMessage;
    }

}
