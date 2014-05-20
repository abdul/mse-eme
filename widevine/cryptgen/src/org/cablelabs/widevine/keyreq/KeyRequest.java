// Confidential material under the terms of the Limited Distribution Non-disclosure
// Agreement between CableLabs and Comcast

package org.cablelabs.widevine.keyreq;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.cablelabs.widevine.Track;

public class KeyRequest {

    
    private static final String POLICY = "";
    private static final String CLIENT_ID = null;
    private static final String[] DRM_TYPES = { "WIDEVINE" };
    
    private static final String CABLELABS_SERVER_URL = "https://license.widevine.com/cenc/getcontentkey/cablelabs";
    private static final String TEST_SERVER_URL      = "https://license.uat.widevine.com";
    
    private static final byte[] CABLELABS_IV = { (byte)0x99, (byte)0xce, (byte)0xac, (byte)0x24,
                                                 (byte)0x52, (byte)0xb5, (byte)0x7b, (byte)0x96,
                                                 (byte)0x5f, (byte)0xed, (byte)0x68, (byte)0x1d,
                                                 (byte)0x8c, (byte)0x49, (byte)0x6b, (byte)0x3e };
    private static final byte[] CABLELABS_KEY = { (byte)0xc3, (byte)0xb0, (byte)0xe5, (byte)0x0a,
                                                  (byte)0xde, (byte)0x0e, (byte)0xde, (byte)0x93,
                                                  (byte)0x6a, (byte)0x53, (byte)0xcb, (byte)0x94,
                                                  (byte)0x5b, (byte)0x80, (byte)0x0a, (byte)0x05,
                                                  (byte)0x28, (byte)0x7f, (byte)0xe1, (byte)0x7a,
                                                  (byte)0x46, (byte)0x36, (byte)0xda, (byte)0x6e,
                                                  (byte)0xed, (byte)0x1c, (byte)0x8c, (byte)0x53,
                                                  (byte)0x65, (byte)0xc0, (byte)0x91, (byte)0x02 };
    
    private String content_id;
    private List<Track> tracks;
    private boolean sign_request = false;
    
    public KeyRequest(String content_id, List<Track> tracks, boolean sign_request)
            throws IllegalArgumentException {
        
        // Validate arguments
        if (content_id == null || content_id.isEmpty())
            throw new IllegalArgumentException("Must provide a valide content ID: " + content_id);
        if (tracks == null || tracks.size() == 0)
            throw new IllegalArgumentException("Must provide a non-empty list of tracks: " +
                                               ((tracks == null) ? "null" : "empty list"));
            
        this.content_id = content_id;
        this.tracks = tracks;
        this.sign_request = sign_request;
    }
    
    public ResponseMessage requestKeys() {
        
        int i;
        
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        Gson prettyGson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

        // Create request object
        RequestMessage requestMessage = new RequestMessage();
        requestMessage.content_id = Base64.encodeBase64String(content_id.getBytes());
        requestMessage.policy = POLICY;
        requestMessage.client_id = CLIENT_ID;
        requestMessage.drm_types = DRM_TYPES;
        
        // Add the track requests to the message
        requestMessage.tracks = new RequestMessage.Track[tracks.size()];
        i = 0;
        for (Track t : tracks) {
            RequestMessage.Track track = new RequestMessage.Track();
            track.type = t.type;
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
        
        String serverURL = null;
        if (sign_request) {
            // Create message signature
            try {
                MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
                sha1.update(jsonRequestMessage.getBytes());
                byte[] sha1_b = sha1.digest();
                System.out.println("SHA-1 hash of JSON request message = 0x" + Hex.encodeHexString(sha1_b));
                
                // Use AES/CBC/PKCS5Padding with CableLabs Key and InitVector
                SecretKeySpec keySpec = new SecretKeySpec(CABLELABS_KEY, "AES");
                IvParameterSpec ivSpec = new IvParameterSpec(CABLELABS_IV);
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                
                // Encrypt the SHA-1 hash of our request message
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
                byte[] encrypted = cipher.doFinal(sha1_b);
                System.out.println("AES/CBC/PKCS5Padding Encrypted SHA1-hash = 0x" + Hex.encodeHexString(encrypted));
                
                request.signer = "cablelabs";
                request.signature = Base64.encodeBase64String(encrypted);
                
                serverURL = CABLELABS_SERVER_URL;
            }
            catch (Exception e) {
                System.out.println("Error performing message encryption!  Message = " + e.getMessage());
                System.exit(1);
            }
        } else {
            request.signer = "widevine_test";
            
            serverURL = TEST_SERVER_URL;
        }
        
        String jsonRequest = gson.toJson(request);
        System.out.println("Request:");
        System.out.println(prettyGson.toJson(request));
        
        String jsonResponseStr = null;
        try {
            
            // Create URL connection
            URL url = new URL(serverURL);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            
            System.out.println("Sending HTTP POST to " + serverURL);
            
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
        
        return responseMessage;
    }
}
